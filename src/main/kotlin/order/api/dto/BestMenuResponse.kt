package order.api.dto

data class BestMenuResponse(
    val menuId: Int,
    val name: String,
    val orderCount: Int = 0,
)