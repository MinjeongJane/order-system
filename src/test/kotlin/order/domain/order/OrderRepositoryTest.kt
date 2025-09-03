package order.domain.order

import order.api.dto.OrderDetailsRequest
import order.api.dto.OrderRequest
import order.domain.event.OrderHistoryEvent
import order.infrastructure.order.OrderHistoryEntity
import order.infrastructure.order.OrderJpaRepository
import order.infrastructure.order.OrderRepositoryImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.util.concurrent.CompletableFuture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult

class OrderRepositoryTest {

    private val orderJpaRepository: OrderJpaRepository = mockk()
    private val kafkaTemplate: KafkaTemplate<String, OrderHistoryEvent> = mockk()
    private lateinit var orderRepository: OrderRepositoryImpl

    @BeforeEach
    fun setUp() {
        orderRepository = spyk(OrderRepositoryImpl(orderJpaRepository, kafkaTemplate))
    }

    @Test
    fun `saveOrder는 주문을 저장하고 OrderHistory를 반환한다`() {
        // given
        val request = OrderRequest(1L, listOf(OrderDetailsRequest(99, 1, 1000)), 1000)
        val entity = mockk<OrderHistoryEntity>()
        val orderHistory = mockk<OrderHistory>()
        every { orderJpaRepository.save(any()) } returns entity
        every { entity.toOrderHistory() } returns orderHistory

        // when
        val result = orderRepository.saveOrder(request)

        // then
        verify { orderJpaRepository.save(any()) }
        assert(result === orderHistory)
    }

    @Test
    fun `publishOrderEvent는 Kafka 전송 확인`() {
        // given
        val request = OrderRequest(1L, listOf(OrderDetailsRequest(99, 1, 1000)), 1000)
        val sendResult = mockk<SendResult<String, OrderHistoryEvent>>()
        val future = CompletableFuture.completedFuture(sendResult)
        every { kafkaTemplate.send(any(), any()) } returns future

        // when
        orderRepository.publishOrderEvent(1L, request)

        // then
        verify { kafkaTemplate.send(any(), any()) }
    }
}