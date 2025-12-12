package order.application.user

import order.domain.user.UserCreditRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserCreditService(
    private val userCreditRepository: UserCreditRepository
) {
    @Transactional
    fun charge(userId: Long, credits: Int) {
        userCreditRepository.chargeCredits(userId, credits)
    }
}