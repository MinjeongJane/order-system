package order.application.order

import order.api.dto.OrderDetailsRequest
import order.api.dto.OrderRequest
import order.domain.best.BestRepository
import order.domain.order.OrderHistory
import order.domain.order.OrderRepository
import order.domain.order.OrderValidation
import order.domain.user.UserCredit
import order.domain.user.UserCreditRepository
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrderServiceTest {

    private lateinit var userCreditRepository: UserCreditRepository
    private lateinit var orderRepository: OrderRepository
    private lateinit var bestRepository: BestRepository
    private lateinit var meterRegistry: MeterRegistry
    private lateinit var orderService: OrderService
    private lateinit var orderValidation: OrderValidation

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        userCreditRepository = mockk()
        orderRepository = mockk()
        bestRepository = mockk()
        meterRegistry = mockk(relaxed = true)
        orderValidation = mockk()
        orderService =
            OrderService(userCreditRepository, orderRepository, bestRepository, meterRegistry, orderValidation)
    }

    @Test
    fun `정상 주문 처리`() = runBlocking {
        // given
        val userId = 1L
        val menuId = 10
        val price = 5000
        val orderDetails = listOf(OrderDetailsRequest(menuId, 1, price))
        val request = OrderRequest(userId, orderDetails, price)
        val user = mockk<UserCredit>()
        val orderHistory = mockk<OrderHistory>(relaxed = true)

        every { orderValidation.validate(request) } returns user
        every { user.chargeCredits(-price) } returns price
        every { bestRepository.increaseOrderCountInRedis(orderDetails) } returns listOf(menuId.toLong(), 2L)
        every { bestRepository.recordMenuStatisticsBatch(orderDetails) } just Runs
        every { userCreditRepository.save(userId, price) } just Runs
        every { orderHistory.id } returns 123L
        every { orderRepository.saveOrder(request) } returns orderHistory
        every { orderRepository.publishOrderEvent(123L, request) } just Runs
        every { meterRegistry.counter(any()) } returns mockk(relaxed = true)

        // when
        val result = orderService.order(request)

        // then
        assertEquals(orderHistory, result)
        verify { bestRepository.increaseOrderCountInRedis(orderDetails) }
        verify { bestRepository.recordMenuStatisticsBatch(orderDetails) }
        verify { userCreditRepository.save(userId, price) }
        verify { orderRepository.publishOrderEvent(123L, request) }
    }

    @Test
    fun `validate 오류 시 예외`() {
        val request = OrderRequest(1L, listOf(OrderDetailsRequest(99, 1, 1000)), 1000)
        every { orderValidation.validate(any()) } throws IllegalArgumentException("주문에 포함된 메뉴가 존재하지 않습니다.")

        val ex = assertThrows<IllegalArgumentException> {
            orderService.order(request)
        }
        assertTrue(ex.message!!.contains("주문 요청이 유효하지 않습니다"))
    }

    @Test
    fun `주문 처리 중 예외 발생시 롤백 및 예외 반환`() {
        val userId = 1L
        val menuId = 10
        val price = 5000
        val orderDetails = listOf(OrderDetailsRequest(menuId, 1, 1000))
        val request = OrderRequest(userId, orderDetails, price)
        val user = mockk<UserCredit>()

        every { orderValidation.validate(request) } returns user
        every { user.chargeCredits(-price) } returns price
        every { userCreditRepository.save(userId, price) } just Runs
        every { bestRepository.increaseOrderCountInRedis(orderDetails) } returns listOf(menuId.toLong(), 2L)
        every { bestRepository.recordMenuStatisticsBatch(orderDetails) } just Runs
        every { orderRepository.saveOrder(request) } throws RuntimeException("DB 오류")
        every { bestRepository.decreaseOrderCountInRedis(orderDetails) } returns listOf(menuId.toLong(), 1L)

        val ex = assertThrows<RuntimeException> {
            orderService.order(request)
        }
        verify { bestRepository.decreaseOrderCountInRedis(orderDetails) }
        assertTrue(ex.message!!.contains("주문 처리 중 오류가 발생했습니다"))
    }
}