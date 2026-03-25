package order.application.user

import order.domain.user.UserCredit
import order.domain.user.UserCreditRepository
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserCreditServiceTest {

    private lateinit var userCreditRepository: UserCreditRepository
    private lateinit var userCreditService: UserCreditService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        userCreditRepository = mockk(relaxed = true)
        userCreditService = UserCreditService(userCreditRepository)
    }

    @Test
    fun `정상적으로 크레딧 충전`() {
        // given
        val userId = 1L
        val credits = 1000
        val user = mockk<UserCredit>()

        every { userCreditRepository.chargeCredits(userId, credits) } just Runs

        // when
        userCreditService.charge(userId, credits)

        // then
        verify { userCreditRepository.chargeCredits(userId, credits) }
    }

    @Test
    fun `존재하지 않는 사용자 예외`() {
        // given
        val userId = 2L
        every { userCreditRepository.chargeCredits(userId, any()) } throws NoSuchElementException("존재하지 않는 사용자입니다.")

        // when & then
        val exception = assertThrows<NoSuchElementException> {
            userCreditService.charge(userId, 100)
        }
        assertEquals("존재하지 않는 사용자입니다.", exception.message)
    }

    @Test
    fun `정상적으로 크레딧 잔액 조회`() {
        // given
        val userId = 1L
        val userCredit = UserCredit(id = userId, credits = 5000)
        every { userCreditRepository.findById(userId) } returns userCredit

        // when
        val result = userCreditService.getBalance(userId)

        // then
        assertEquals(userId, result.id)
        assertEquals(5000, result.credits)
    }

    @Test
    fun `존재하지 않는 사용자 잔액 조회 예외`() {
        // given
        val userId = 999L
        every { userCreditRepository.findById(userId) } returns null

        // when & then
        val exception = assertThrows<NoSuchElementException> {
            userCreditService.getBalance(userId)
        }
        assertEquals("존재하지 않는 사용자입니다.", exception.message)
    }
}