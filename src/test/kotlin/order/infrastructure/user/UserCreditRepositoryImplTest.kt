package order.infrastructure.user

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.assertThrows
import java.util.Optional

class UserCreditRepositoryImplTest : DescribeSpec({

    val userCreditJpaRepository = mockk<UserCreditJpaRepository>()
    val userCreditRepositoryImpl = UserCreditRepositoryImpl(userCreditJpaRepository)

    describe("findById") {
        context("사용자가 존재할 때") {
            it("UserCredit 도메인 객체를 반환한다") {
                // given
                val entity = UserCreditEntity(id = 1L, credits = 5000)
                every { userCreditJpaRepository.findById(1L) } returns Optional.of(entity)

                // when
                val result = userCreditRepositoryImpl.findById(1L)

                // then
                result shouldNotBe null
                result!!.id shouldBe 1L
                result.credits shouldBe 5000
            }
        }

        context("사용자가 존재하지 않을 때") {
            it("null을 반환한다") {
                // given
                every { userCreditJpaRepository.findById(999L) } returns Optional.empty()

                // when
                val result = userCreditRepositoryImpl.findById(999L)

                // then
                result shouldBe null
            }
        }
    }

    describe("chargeCredits") {
        context("사용자가 존재할 때") {
            it("크레딧을 업데이트한다") {
                // given
                val entity = UserCreditEntity(id = 1L, credits = 1000)
                every { userCreditJpaRepository.findByIdWithLock(1L) } returns entity

                // when
                userCreditRepositoryImpl.chargeCredits(1L, 500)

                // then
                entity.credits shouldBe 1500
                verify(exactly = 1) { userCreditJpaRepository.findByIdWithLock(1L) }
            }
        }

        context("사용자가 존재하지 않을 때") {
            it("NoSuchElementException을 발생시킨다") {
                // given
                every { userCreditJpaRepository.findByIdWithLock(999L) } returns null

                // when & then
                val exception = assertThrows<NoSuchElementException> {
                    userCreditRepositoryImpl.chargeCredits(999L, 100)
                }
                exception.message shouldBe "존재하지 않는 사용자입니다."
            }
        }
    }

    describe("save") {
        context("유효한 사용자 ID와 크레딧이 주어졌을 때") {
            it("엔티티를 저장한다") {
                // given
                val entity = UserCreditEntity(id = 1L, credits = 3000)
                every { userCreditJpaRepository.save(any()) } returns entity

                // when
                userCreditRepositoryImpl.save(1L, 3000)

                // then
                verify(exactly = 1) { userCreditJpaRepository.save(any()) }
            }
        }
    }
})
