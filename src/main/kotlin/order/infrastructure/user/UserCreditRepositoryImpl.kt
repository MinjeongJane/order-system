package order.infrastructure.user

import order.domain.user.UserCredit
import order.domain.user.UserCreditRepository
import org.springframework.stereotype.Repository

@Repository
class UserCreditRepositoryImpl(
    private val userCreditJpaRepository: UserCreditJpaRepository
) : UserCreditRepository {
    override fun findById(id: Long): UserCredit? {
        return userCreditJpaRepository.findById(id)
            .map { it.toUser() }
            .orElse(null)
    }

    override fun chargeCredits(id: Long, credits: Int) {
        val userCredits = userCreditJpaRepository.findByIdWithLock(id)
            ?: throw NoSuchElementException("존재하지 않는 사용자입니다.")

        userCredits.credits += credits
    }

    override fun save(userId: Long, credits: Int) {
        userCreditJpaRepository.save(UserCreditEntity(id = userId, credits = credits))
    }
}