package order.domain.order

import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import order.api.dto.OrderDetailsRequest
import order.api.dto.OrderRequest
import order.domain.menu.MenuRepository
import order.domain.user.UserCredit
import order.domain.user.UserCreditRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrderValidationTest {

    private val menuRepository: MenuRepository = mockk()
    private val userCreditRepository: UserCreditRepository = mockk()
    private val orderValidation = OrderValidation(menuRepository, userCreditRepository)

    @Test
    fun `validate - 성공적으로 검증`() {
        // given
        val orderDetails = listOf(OrderDetailsRequest(menuId = 1, count = 2, menuPrice = 5000))
        val request = OrderRequest(userId = 1L, orderDetails = orderDetails, price = 1000)
        every { menuRepository.existsByIds(listOf(1)) } returns true
        val userPoint = UserCredit(1L, 2000)
        every { userCreditRepository.findById(1L) } returns userPoint

        // when
        val result = orderValidation.validate(request)

        // then
        assertEquals(userPoint, result)
    }

    @Test
    fun `validate - 존재하지 않는 메뉴 예외`() {
        // given
        val orderDetails = listOf(OrderDetailsRequest(menuId = 1, count = 2, menuPrice = 5000))
        val request = OrderRequest(userId = 1L, orderDetails = orderDetails, price = 1000)
        every { menuRepository.existsByIds(listOf(1)) } returns false

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            orderValidation.validate(request)
        }
        assertEquals("주문에 포함된 메뉴가 존재하지 않습니다.", exception.message)
    }

    @Test
    fun `validate - 존재하지 않는 사용자 예외`() {
        // given
        val orderDetails = listOf(OrderDetailsRequest(menuId = 1, count = 2, menuPrice = 5000))
        val request = OrderRequest(userId = 1L, orderDetails = orderDetails, price = 1000)
        every { menuRepository.existsByIds(listOf(1)) } returns true
        every { userCreditRepository.findById(1L) } returns null

        // when & then
        val exception = assertThrows<NoSuchElementException> {
            orderValidation.validate(request)
        }
        assertEquals("존재하지 않는 사용자입니다.", exception.message)
    }

    @Test
    fun `validate - 포인트 부족 예외`() {
        // given
        val orderDetails = listOf(OrderDetailsRequest(menuId = 1, count = 2, menuPrice = 5000))
        val request = OrderRequest(userId = 1L, orderDetails = orderDetails, price = 3000)
        every { menuRepository.existsByIds(listOf(1)) } returns true
        val userPoint = UserCredit(1L, 2000)
        every { userCreditRepository.findById(1L) } returns userPoint

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            orderValidation.validate(request)
        }
        assertEquals("사용자의 크레딧이 부족합니다.", exception.message)
    }
}