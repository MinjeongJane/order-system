package order.infrastructure.order

import order.api.dto.OrderRequest
import order.domain.event.OrderDetailsEvent
import order.domain.event.OrderHistoryEvent
import order.domain.order.OrderHistory
import order.domain.order.OrderRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Repository

@Repository
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
    private val kafkaTemplate: KafkaTemplate<String, OrderHistoryEvent>
) : OrderRepository {
    private val logger = KotlinLogging.logger {}

    override fun saveOrder(request: OrderRequest): OrderHistory {
        val orderHistory = OrderHistoryEntity(
            userId = request.userId,
            price = request.price,
        )

        request.orderDetails.forEach { orderDetails ->
            orderHistory.addDetails(orderDetails.menuId, orderDetails.count, orderDetails.menuPrice)
        }

        // 주문 내역 저장
        val savedEntity = orderJpaRepository.save(orderHistory)

        return savedEntity.toOrderHistory()
    }

    override fun publishOrderEvent(id: Long, request: OrderRequest) {
        val event = OrderHistoryEvent(
            orderId = id,
            userId = request.userId,
            price = request.price,
            orderDetails = request.orderDetails.map {
                OrderDetailsEvent(it.menuId, it.count)
            }
        )
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, event)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info { "Kafka 전송 성공: ${result?.recordMetadata}" }
                } else {
                    logger.error(ex) { "Kafka 전송 실패" }
                }
            }
    }

    companion object {
        private const val ORDER_EVENTS_TOPIC = "order-events"
    }
}
