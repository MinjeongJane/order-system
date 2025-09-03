package order.infrastructure.feign.openai.dto

data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null
) {
    data class Choice(val message: OpenAiMessageContent?)
    data class OpenAiMessageContent(val role: String?, val content: String?)
    data class Usage(val prompt_tokens: Int?, val completion_tokens: Int?)
}