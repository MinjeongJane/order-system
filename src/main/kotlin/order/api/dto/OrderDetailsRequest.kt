package order.api.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class OrderDetailsRequest(
    @field:NotNull
    val menuId: Int,

    @field:Min(1)
    val count: Int,

    @field:Min(1)
    val menuPrice: Int,
)