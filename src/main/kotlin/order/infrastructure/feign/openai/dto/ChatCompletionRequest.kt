package order.infrastructure.feign.openai.dto

import order.infrastructure.recommend.OpenAiMessage

data class ChatCompletionRequest(
    val model: String,
    val temperature: Double? = null,
    val messages: List<OpenAiMessage>
)