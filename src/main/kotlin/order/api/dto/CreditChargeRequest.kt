package order.api.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class CreditChargeRequest(
    @field:NotNull
    val userId: Long,

    @field:Min(1)
    val credits: Int,
)