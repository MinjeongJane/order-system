package order.application.order

import order.api.dto.OrderRequest
import order.common.config.DistributedLock
import order.domain.best.BestRepository
import order.domain.order.OrderHistory
import order.domain.order.OrderRepository
import order.domain.order.OrderValidation
import order.domain.user.UserCreditRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(
    private val userCreditRepository: UserCreditRepository,
    private val orderRepository: OrderRepository,
    private val bestRepository: BestRepository,
    private val meterRegistry: MeterRegistry,
    private val orderValidation: OrderValidation,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 아이스크림 메뉴 주문
     * 1. 유효성 검사
     * 2. 메뉴주문 카운트 저장
     * 3. 포인트 차감 및 주문
     * 4. event 발행
     */
    @DistributedLock(
        key = LOCK_KEY,
        keyExpression = LOCK_KEY_EXPRESSION,
        waitTime = ORDER_WAIT_TIME,
        leaseTime = ORDER_LEASE_TIME,
        failureMessage = ORDER_FAILURE_MESSAGE
    )
    fun order(request: OrderRequest): OrderHistory {
        try {
            // 유효성 검사
            val user = orderValidation.validate(request)

            // 크레딧 차감 및 주문
            val orderHistory = performAtomicOrder(request, user.chargeCredits(-request.price))

            // 주문 이벤트 발행
            orderRepository.publishOrderEvent(id = orderHistory.id, request = request)

            // 메트릭 수집
            request.orderDetails.forEach {
                meterRegistry.counter(METRIC_NAME, METRIC_TAGS, it.menuId.toString()).increment(it.count.toDouble())
            }

            return orderHistory
        } catch (e: Exception) {
            when (e) {
                is NoSuchElementException, is IllegalArgumentException -> {
                    logger.warn { "주문 요청값 오류 : ${e.message}" }
                    throw IllegalArgumentException("주문 요청이 유효하지 않습니다: ${e.message}")
                }

                else -> {
                    bestRepository.decreaseOrderCountInRedis(request.orderDetails)

                    logger.error { "주문 처리중 오류 : ${e.message}" }
                    throw RuntimeException("주문 처리 중 오류가 발생했습니다: ${e.message}", e)
                }
            }
        }
    }

    @Transactional
    internal fun performAtomicOrder(request: OrderRequest, credits: Int): OrderHistory {
        // redis 및 DB 주문 카운트 업데이트
        bestRepository.increaseOrderCountInRedis(request.orderDetails)
        bestRepository.recordMenuStatisticsBatch(request.orderDetails)

        userCreditRepository.save(request.userId, credits)
        return orderRepository.saveOrder(request)
    }

    companion object {
        const val LOCK_KEY = "order:processing"
        const val LOCK_KEY_EXPRESSION = "#request.userId"
        const val ORDER_WAIT_TIME = 3L
        const val ORDER_LEASE_TIME = 30L
        const val ORDER_FAILURE_MESSAGE = "주문 처리 중입니다. 잠시 후 다시 시도해주세요."

        const val METRIC_NAME = "ordersystem_orders_by_menu"
        const val METRIC_TAGS = "menuId"
    }
}