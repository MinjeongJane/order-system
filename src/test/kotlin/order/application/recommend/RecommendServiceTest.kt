package order.application.recommend

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import order.api.dto.RecommendRequest
import order.api.dto.RecommendResponse
import order.domain.recommend.RecommendRepository

class RecommendServiceTest : DescribeSpec({

    val recommendRepository = mockk<RecommendRepository>()
    val recommendService = RecommendService(recommendRepository)

    describe("recommend") {
        context("유효한 추천 요청이 주어졌을 때") {
            it("추천 결과를 반환한다") {
                // given
                val request = RecommendRequest(userMessage = "달콤한 아이스크림 추천해줘")
                val expectedResponse = RecommendResponse(
                    recommendation = "바닐라 아이스크림을 추천드립니다.",
                    promptTokens = 10,
                    completionTokens = 20
                )
                every { recommendRepository.recommend(request) } returns expectedResponse

                // when
                val result = recommendService.recommend(request)

                // then
                result shouldBe expectedResponse
                result.recommendation shouldBe "바닐라 아이스크림을 추천드립니다."
                verify(exactly = 1) { recommendRepository.recommend(request) }
            }
        }

        context("대화 이력이 포함된 요청이 주어졌을 때") {
            it("대화 이력을 포함하여 추천 결과를 반환한다") {
                // given
                val request = RecommendRequest(
                    userMessage = "다른 것도 추천해줘",
                    history = emptyList()
                )
                val expectedResponse = RecommendResponse(
                    recommendation = "초코 아이스크림도 좋습니다.",
                    promptTokens = 15,
                    completionTokens = 25
                )
                every { recommendRepository.recommend(request) } returns expectedResponse

                // when
                val result = recommendService.recommend(request)

                // then
                result shouldBe expectedResponse
                verify(exactly = 1) { recommendRepository.recommend(request) }
            }
        }
    }
})
