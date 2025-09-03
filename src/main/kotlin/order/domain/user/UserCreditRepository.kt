package order.domain.user

interface UserCreditRepository {
    fun findById(id: Long): UserCredit?

    fun chargeCredits(id: Long, credits: Int)

    fun save(userId: Long, credits: Int)
}