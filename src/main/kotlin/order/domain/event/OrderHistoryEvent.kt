package order.domain.event

import java.time.LocalDateTime

data class OrderHistoryEvent(
    val orderId: Long,
    val userId: Long,
    val price: Int,
    val orderDetails: List<OrderDetailsEvent>,
    val createdDate: LocalDateTime = LocalDateTime.now()
)
