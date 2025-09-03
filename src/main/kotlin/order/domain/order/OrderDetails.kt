package order.domain.order

import java.time.LocalDateTime

data class OrderDetails(
    val id: Long,
    val orderId: OrderHistory,
    val menuId: Int,
    val count: Int,
    val menuPrice: Int,
    val createdDate: LocalDateTime,
    val updatedDate: LocalDateTime,
)