package order.api

import order.api.dto.RecommendRequest
import order.api.dto.RecommendResponse
import order.application.recommend.RecommendService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ai")
class RecommendController(
    private val recommendService: RecommendService,
) {
    // 아이스크림 메뉴 추천
    @PostMapping("/recommend")
    fun recommend(@RequestBody req: RecommendRequest): RecommendResponse = recommendService.recommend(req)
}