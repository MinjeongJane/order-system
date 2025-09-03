package order.domain.user

data class UserCredit(
    val id: Long,
    val credits: Int,
) {
    fun chargeCredits(amount: Int): UserCredit {
        return this.copy(credits = credits + amount)
    }

    fun canPay(amount: Int): Boolean {
        return credits >= amount
    }
}
