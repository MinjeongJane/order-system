package order.application.user

import order.domain.user.UserCreditRepository
import org.springframework.stereotype.Service

@Service
class UserCreditService(
    private val userCreditRepository: UserCreditRepository
) {
    fun charge(userId: Long, credits: Int) {
        userCreditRepository.chargeCredits(userId, credits)
    }
}