package order.domain.recommend

import order.api.dto.RecommendRequest
import order.api.dto.RecommendResponse

interface RecommendRepository {
    fun recommend(req: RecommendRequest): RecommendResponse
}
