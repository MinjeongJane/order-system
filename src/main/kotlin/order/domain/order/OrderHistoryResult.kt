package order.domain.order

data class OrderHistoryResult(
    val history: OrderHistory,
    val details: List<OrderDetails>,
)