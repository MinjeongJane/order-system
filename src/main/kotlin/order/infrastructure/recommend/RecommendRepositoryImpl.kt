package order.infrastructure.recommend

import order.api.dto.RecommendRequest
import order.api.dto.RecommendResponse
import order.domain.recommend.RecommendRepository
import order.infrastructure.feign.openai.OpenAiFeignClient
import order.infrastructure.feign.openai.dto.ChatCompletionRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

@Repository
class RecommendRepositoryImpl(
    private val openAi: OpenAiFeignClient,
    @Value("\${openai.model}") private val model: String,
    @Value("\${openai.temperature}") private val temperature: Double
) : RecommendRepository {
    override fun recommend(req: RecommendRequest): RecommendResponse {
        // 1) system role로 AI의 역할/톤 고정
        val systemMsg = OpenAiMessage(
            role = "system",
            content = """
                You are a friendly, concise icecream maker assistant.
                Recommend 1-2 icecream items from the given menu.
                Include a short reason and, if relevant, a customization tip (flavor, fruity, calory).
                Reply in Korean.
            """.trimIndent()
        )

        // 2) user role: 메뉴 + 사용자의 상황/취향 메시지
        val menuList = listOf("바닐라", "초코", "딸기", "망고")
        val userMsg = OpenAiMessage(
            role = "user",
            content = "Menu: [$menuList]\nUser says: ${req.userMessage}"
        )

        // 3) history가 있으면 그대로 이어붙이기(MCP 멀티턴)
        val historyMsgs: List<OpenAiMessage> = (req.history ?: emptyList()).map {
            OpenAiMessage(role = it.role, content = it.content)
        }

        val messages = listOf(systemMsg) + historyMsgs + userMsg

        val payload = ChatCompletionRequest(
            model = model,
            temperature = temperature,
            messages = messages
        )

        val res = openAi.chatCompletions(payload)
        val content = res.choices.firstOrNull()?.message?.content?.trim().orEmpty()

        return RecommendResponse(
            recommendation = content,
            promptTokens = res.usage?.prompt_tokens,
            completionTokens = res.usage?.completion_tokens
        )
    }
}