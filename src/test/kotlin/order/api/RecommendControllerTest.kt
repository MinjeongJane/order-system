package order.api

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import order.api.dto.RecommendRequest
import order.api.dto.RecommendResponse
import order.application.recommend.RecommendService

class RecommendControllerTest : DescribeSpec({

    val recommendService = mockk<RecommendService>()
    val recommendController = RecommendController(recommendService)

    describe("recommend") {
        context("유효한 추천 요청이 주어졌을 때") {
            it("추천 결과를 그대로 반환한다") {
                // given
                val request = RecommendRequest(userMessage = "달콤한 것 추천해줘")
                val expectedResponse = RecommendResponse(
                    recommendation = "바닐라 아이스크림을 추천드립니다.",
                    promptTokens = 10,
                    completionTokens = 20
                )
                every { recommendService.recommend(request) } returns expectedResponse

                // when
                val result = recommendController.recommend(request)

                // then
                result shouldBe expectedResponse
                result.recommendation shouldBe "바닐라 아이스크림을 추천드립니다."
                verify(exactly = 1) { recommendService.recommend(request) }
            }
        }
    }
})
