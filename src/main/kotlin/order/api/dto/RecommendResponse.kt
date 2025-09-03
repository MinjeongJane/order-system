package order.api.dto

data class RecommendResponse(
    val recommendation: String,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null
)