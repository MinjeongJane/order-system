package order.infrastructure.recommend

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import order.api.dto.McpMessage
import order.api.dto.RecommendRequest
import order.infrastructure.feign.openai.OpenAiFeignClient
import order.infrastructure.feign.openai.dto.ChatCompletionRequest
import order.infrastructure.feign.openai.dto.ChatCompletionResponse

class RecommendRepositoryImplTest : DescribeSpec({

    val openAiFeignClient = mockk<OpenAiFeignClient>()
    val model = "gpt-3.5-turbo"
    val temperature = 0.7

    val recommendRepositoryImpl = RecommendRepositoryImpl(openAiFeignClient, model, temperature)

    describe("recommend") {
        context("사용자 메시지만 있는 요청이 주어졌을 때") {
            it("OpenAI API를 호출하고 추천 결과를 반환한다") {
                // given
                val request = RecommendRequest(userMessage = "달콤한 아이스크림 추천해줘")
                val apiResponse = ChatCompletionResponse(
                    choices = listOf(
                        ChatCompletionResponse.Choice(
                            message = ChatCompletionResponse.OpenAiMessageContent(
                                role = "assistant",
                                content = "바닐라 아이스크림을 추천드립니다!"
                            )
                        )
                    ),
                    usage = ChatCompletionResponse.Usage(
                        prompt_tokens = 50,
                        completion_tokens = 30
                    )
                )
                every { openAiFeignClient.chatCompletions(any()) } returns apiResponse

                // when
                val result = recommendRepositoryImpl.recommend(request)

                // then
                result shouldNotBe null
                result.recommendation shouldBe "바닐라 아이스크림을 추천드립니다!"
                result.promptTokens shouldBe 50
                result.completionTokens shouldBe 30
                verify(exactly = 1) { openAiFeignClient.chatCompletions(any()) }
            }
        }

        context("대화 이력이 포함된 요청이 주어졌을 때") {
            it("이력을 포함하여 OpenAI API를 호출한다") {
                // given
                val history = listOf(
                    McpMessage(role = "user", content = "이전 메시지"),
                    McpMessage(role = "assistant", content = "이전 응답")
                )
                val request = RecommendRequest(
                    userMessage = "다른 것도 추천해줘",
                    history = history
                )
                val apiResponse = ChatCompletionResponse(
                    choices = listOf(
                        ChatCompletionResponse.Choice(
                            message = ChatCompletionResponse.OpenAiMessageContent(
                                role = "assistant",
                                content = "초코 아이스크림도 좋습니다."
                            )
                        )
                    ),
                    usage = ChatCompletionResponse.Usage(
                        prompt_tokens = 70,
                        completion_tokens = 20
                    )
                )
                every { openAiFeignClient.chatCompletions(any()) } returns apiResponse

                // when
                val result = recommendRepositoryImpl.recommend(request)

                // then
                result.recommendation shouldBe "초코 아이스크림도 좋습니다."
                verify(exactly = 1) {
                    openAiFeignClient.chatCompletions(match { req: ChatCompletionRequest ->
                        // system(1) + history(2) + user(1) = 4 messages
                        req.messages.size == 4
                    })
                }
            }
        }

        context("API 응답에 choices가 없을 때") {
            it("빈 추천 결과를 반환한다") {
                // given
                val request = RecommendRequest(userMessage = "추천해줘")
                val apiResponse = ChatCompletionResponse(
                    choices = emptyList(),
                    usage = null
                )
                every { openAiFeignClient.chatCompletions(any()) } returns apiResponse

                // when
                val result = recommendRepositoryImpl.recommend(request)

                // then
                result.recommendation shouldBe ""
                result.promptTokens shouldBe null
                result.completionTokens shouldBe null
            }
        }

        context("API 응답의 message content가 null일 때") {
            it("빈 추천 결과를 반환한다") {
                // given
                val request = RecommendRequest(userMessage = "추천해줘")
                val apiResponse = ChatCompletionResponse(
                    choices = listOf(
                        ChatCompletionResponse.Choice(message = null)
                    ),
                    usage = null
                )
                every { openAiFeignClient.chatCompletions(any()) } returns apiResponse

                // when
                val result = recommendRepositoryImpl.recommend(request)

                // then
                result.recommendation shouldBe ""
            }
        }
    }
})
