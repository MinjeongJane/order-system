package order.api.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class MenuRequest(
    val id: Long?,

    @field:NotNull
    val name: String?,

    @field:Min(0)
    val price: Int?,
)