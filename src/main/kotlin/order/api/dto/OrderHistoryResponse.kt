package order.api.dto

import java.time.LocalDateTime

data class OrderHistoryResponse(
    val orderId: Long,
    val price: Int,
    val createdAt: LocalDateTime,
    val details: List<OrderDetailResponse>,
)

data class OrderDetailResponse(
    val menuId: Int,
    val count: Int,
    val menuPrice: Int,
)