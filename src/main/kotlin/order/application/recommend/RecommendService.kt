package order.application.recommend

import order.api.dto.RecommendRequest
import order.api.dto.RecommendResponse
import order.domain.recommend.RecommendRepository
import org.springframework.stereotype.Service

@Service
class RecommendService(
    private val recommendRepository: RecommendRepository,
) {
    fun recommend(req: RecommendRequest): RecommendResponse {
        return recommendRepository.recommend(req)
    }
}