package order.api.dto

data class CreditBalanceResponse(
    val userId: Long,
    val credits: Int,
)