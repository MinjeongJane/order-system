package order.infrastructure.order

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import order.api.dto.OrderDetailsRequest
import order.api.dto.OrderRequest
import order.domain.event.OrderHistoryEvent
import order.domain.order.OrderHistory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class OrderRepositoryImplTest : DescribeSpec({

    val orderJpaRepository = mockk<OrderJpaRepository>()
    val kafkaTemplate = mockk<KafkaTemplate<String, OrderHistoryEvent>>()
    val orderRepositoryImpl = OrderRepositoryImpl(orderJpaRepository, kafkaTemplate)

    afterTest { clearAllMocks() }

    describe("saveOrder") {
        context("유효한 주문 요청이 주어졌을 때") {
            it("주문 내역 엔티티를 저장하고 도메인 객체로 반환한다") {
                // given
                val orderDetails = listOf(OrderDetailsRequest(menuId = 1, count = 2, menuPrice = 3000))
                val request = OrderRequest(userId = 1L, orderDetails = orderDetails, price = 6000)

                val now = LocalDateTime.now()
                val expectedOrderHistory = OrderHistory(
                    id = 100L,
                    userId = 1L,
                    price = 6000,
                    createdBy = "system",
                    createdAt = now,
                    modifiedBy = "system",
                    modifiedAt = now
                )
                val savedEntity = mockk<OrderHistoryEntity>()
                every { savedEntity.toOrderHistory() } returns expectedOrderHistory
                every { orderJpaRepository.save(any()) } returns savedEntity

                // when
                val result = orderRepositoryImpl.saveOrder(request)

                // then
                result shouldNotBe null
                result.userId shouldBe 1L
                result.price shouldBe 6000
                result.id shouldBe 100L
                verify(exactly = 1) { orderJpaRepository.save(any()) }
            }
        }
    }

    describe("publishOrderEvent") {
        context("카프카 전송이 성공할 때") {
            it("Kafka 이벤트를 발행한다") {
                // given
                val orderDetails = listOf(OrderDetailsRequest(menuId = 1, count = 1, menuPrice = 3000))
                val request = OrderRequest(userId = 1L, orderDetails = orderDetails, price = 3000)
                val sendResult = mockk<SendResult<String, OrderHistoryEvent>>(relaxed = true)
                val future = CompletableFuture.completedFuture(sendResult)

                every { kafkaTemplate.send(any<String>(), any<OrderHistoryEvent>()) } returns future

                // when
                orderRepositoryImpl.publishOrderEvent(id = 100L, request = request)

                // then
                verify(exactly = 1) { kafkaTemplate.send(any<String>(), any<OrderHistoryEvent>()) }
            }
        }

        context("카프카 전송이 실패할 때") {
            it("예외를 로깅하고 메서드가 정상 종료된다") {
                // given
                val orderDetails = listOf(OrderDetailsRequest(menuId = 2, count = 1, menuPrice = 4000))
                val request = OrderRequest(userId = 2L, orderDetails = orderDetails, price = 4000)
                val failedFuture = CompletableFuture<SendResult<String, OrderHistoryEvent>>()
                failedFuture.completeExceptionally(RuntimeException("Kafka 연결 실패"))

                every { kafkaTemplate.send(any<String>(), any<OrderHistoryEvent>()) } returns failedFuture

                // when & then (whenComplete로 처리되므로 예외가 전파되지 않아야 한다)
                orderRepositoryImpl.publishOrderEvent(id = 200L, request = request)

                verify(exactly = 1) { kafkaTemplate.send(any<String>(), any<OrderHistoryEvent>()) }
            }
        }
    }
})
