package order.domain.event

data class OrderDetailsEvent(
    val menuId: Int,
    val quantity: Int
)