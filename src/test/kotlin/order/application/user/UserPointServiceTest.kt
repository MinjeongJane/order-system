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

class UserPointServiceTest {

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
        val points = 1000
        val user = mockk<UserCredit>()

        every { userCreditRepository.chargeCredits(userId, points) } just Runs

        // when
        userCreditService.charge(userId, points)

        // then
        verify { userCreditRepository.chargeCredits(userId, points) }
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
}