package order.api.dto

data class McpMessage(
    val role: String,
    val content: String,
    val metadata: Map<String, Any?>? = null
)