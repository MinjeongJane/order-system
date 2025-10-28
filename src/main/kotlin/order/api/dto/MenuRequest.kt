package order.api.dto

import jakarta.validation.constraints.Min

data class MenuRequest(
    val id: Long?,

    val name: String?,

    @field:Min(value = 0, message = "가격설정은 0 이상이어야 합니다.")
    val price: Int?,
)