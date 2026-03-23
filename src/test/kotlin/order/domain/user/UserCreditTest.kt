package order.domain.user

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class UserCreditTest : DescribeSpec({

    describe("chargeCredits") {
        context("양수 금액을 충전할 때") {
            it("크레딧이 증가한다") {
                // given
                val userCredit = UserCredit(id = 1L, credits = 1000)

                // when
                val result = userCredit.chargeCredits(500)

                // then
                result shouldBe 1500
            }
        }

        context("음수 금액을 전달할 때 (결제 차감)") {
            it("크레딧이 감소한다") {
                // given
                val userCredit = UserCredit(id = 1L, credits = 1000)

                // when
                val result = userCredit.chargeCredits(-300)

                // then
                result shouldBe 700
            }
        }

        context("0원을 충전할 때") {
            it("크레딧이 변하지 않는다") {
                // given
                val userCredit = UserCredit(id = 1L, credits = 1000)

                // when
                val result = userCredit.chargeCredits(0)

                // then
                result shouldBe 1000
            }
        }
    }

    describe("canPay") {
        context("크레딧이 결제 금액 이상일 때") {
            it("true를 반환한다") {
                // given
                val userCredit = UserCredit(id = 1L, credits = 5000)

                // when & then
                userCredit.canPay(5000) shouldBe true
                userCredit.canPay(3000) shouldBe true
            }
        }

        context("크레딧이 결제 금액보다 부족할 때") {
            it("false를 반환한다") {
                // given
                val userCredit = UserCredit(id = 1L, credits = 2000)

                // when & then
                userCredit.canPay(3000) shouldBe false
            }
        }

        context("크레딧이 0일 때") {
            it("0원 결제만 가능하다") {
                // given
                val userCredit = UserCredit(id = 1L, credits = 0)

                // when & then
                userCredit.canPay(0) shouldBe true
                userCredit.canPay(1) shouldBe false
            }
        }
    }
})
