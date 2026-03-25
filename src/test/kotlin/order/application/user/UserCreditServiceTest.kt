package order.application.user

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import order.domain.user.UserCreditRepository

class UserCreditServiceTest : DescribeSpec({

    val userCreditRepository = mockk<UserCreditRepository>()
    val userCreditService = UserCreditService(userCreditRepository)

    describe("charge") {
        context("유효한 사용자에게 크레딧 충전 요청 시") {
            it("크레딧 충전이 정상적으로 처리된다") {
                val userId = 1L
                val credits = 1000
                every { userCreditRepository.chargeCredits(userId, credits) } returns Unit

                userCreditService.charge(userId, credits)

                verify(exactly = 1) { userCreditRepository.chargeCredits(userId, credits) }
            }
        }

        context("존재하지 않는 사용자에게 크레딧 충전 요청 시") {
            it("NoSuchElementException 이 발생한다") {
                val userId = 2L
                every { userCreditRepository.chargeCredits(userId, any()) } throws NoSuchElementException("존재하지 않는 사용자입니다.")

                val exception = shouldThrow<NoSuchElementException> {
                    userCreditService.charge(userId, 100)
                }
                exception.message shouldBe "존재하지 않는 사용자입니다."
            }
        }
    }
})
