package order.api.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class OrderRequest(
    @field:NotNull
    val userId: Long,

    @field:NotNull
    val orderDetails: List<OrderDetailsRequest>,

    @field:Min(1)
    val price: Int,
)
