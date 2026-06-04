package order.application.user

import order.domain.user.UserCredit
import order.domain.user.UserCreditRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UserCreditServiceTest : DescribeSpec({

    val userCreditRepository = mockk<UserCreditRepository>(relaxed = true)
    val userCreditService = UserCreditService(userCreditRepository)

    describe("charge") {
        context("정상적인 userId와 크레딧이 주어졌을 때") {
            it("chargeCredits를 호출한다") {
                // given
                val userId = 1L
                val credits = 1000

                // when
                userCreditService.charge(userId, credits)

                // then
                verify { userCreditRepository.chargeCredits(userId, credits) }
            }
        }

        context("존재하지 않는 사용자 userId가 주어졌을 때") {
            it("NoSuchElementException을 던진다") {
                // given
                val userId = 2L
                every { userCreditRepository.chargeCredits(userId, any()) } throws NoSuchElementException("존재하지 않는 사용자입니다.")

                // when & then
                val exception = shouldThrow<NoSuchElementException> {
                    userCreditService.charge(userId, 100)
                }
                exception.message shouldBe "존재하지 않는 사용자입니다."
            }
        }
    }

    describe("getBalance") {
        context("존재하는 userId가 주어졌을 때") {
            it("UserCredit을 반환한다") {
                // given
                val userId = 1L
                val userCredit = UserCredit(id = userId, credits = 5000)
                every { userCreditRepository.findById(userId) } returns userCredit

                // when
                val result = userCreditService.getBalance(userId)

                // then
                result.id shouldBe userId
                result.credits shouldBe 5000
            }
        }

        context("존재하지 않는 userId가 주어졌을 때") {
            it("NoSuchElementException을 던진다") {
                // given
                val userId = 999L
                every { userCreditRepository.findById(userId) } returns null

                // when & then
                val exception = shouldThrow<NoSuchElementException> {
                    userCreditService.getBalance(userId)
                }
                exception.message shouldBe "존재하지 않는 사용자입니다."
            }
        }
    }
})