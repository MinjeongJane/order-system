package order.domain.order

import java.time.LocalDateTime

data class OrderHistory(
    val id: Long,
    val userId: Long,
    val price: Int,
    val createdDate: LocalDateTime,
    val updatedDate: LocalDateTime,
)