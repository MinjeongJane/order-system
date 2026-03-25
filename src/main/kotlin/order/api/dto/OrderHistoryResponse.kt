package order.api.dto

import java.time.LocalDateTime

data class OrderHistoryResponse(
    val orderId: Long,
    val userId: Long,
    val price: Int,
    val createdAt: LocalDateTime,
)
