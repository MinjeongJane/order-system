package order.application.user

import order.domain.user.UserCredit
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

    @Transactional(readOnly = true)
    fun getBalance(userId: Long): UserCredit {
        return userCreditRepository.findById(userId)
            ?: throw NoSuchElementException("존재하지 않는 사용자입니다.")
    }
}