package order.api.dto

data class RecommendRequest(
    val userMessage: String,
    val history: List<McpMessage>? = null
)