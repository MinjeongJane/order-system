package order.application.graph

import order.domain.graph.AllergyConflict
import order.domain.graph.CoOrderedMenu
import order.domain.graph.GraphRecommendation
import order.domain.graph.GraphSyncResult
import order.domain.graph.MenuPopularity
import order.domain.graph.OrderGraphRepository
import order.domain.graph.OrderGraphSyncRepository
import order.domain.graph.SimilarUser
import order.domain.graph.RecommendStrategy
import order.domain.graph.RecommendationResult
import order.domain.graph.SocialRecommendation
import order.domain.graph.UserConnection
import order.domain.graph.UserMenuTaste
import org.springframework.stereotype.Service

@Service
class OrderGraphService(
    private val orderGraphRepository: OrderGraphRepository,
    private val orderGraphSyncRepository: OrderGraphSyncRepository,
) {
    fun sync(): GraphSyncResult = orderGraphSyncRepository.syncAll()

    fun findUserTaste(userId: Int): List<UserMenuTaste> =
        orderGraphRepository.findUserTaste(userId)

    fun findCoOrderedMenus(limit: Int): List<CoOrderedMenu> =
        orderGraphRepository.findCoOrderedMenus(limit)

    fun findSimilarUsers(userId: Int, limit: Int): List<SimilarUser> =
        orderGraphRepository.findSimilarUsers(userId, limit)

    fun recommendMenus(userId: Int, limit: Int): List<GraphRecommendation> =
        orderGraphRepository.recommendMenus(userId, limit)

    fun findConnection(fromUserId: Int, toUserId: Int): UserConnection? =
        orderGraphRepository.findConnection(fromUserId, toUserId)

    fun findMenuPopularity(limit: Int): List<MenuPopularity> =
        orderGraphRepository.findMenuPopularity(limit)

    /**
     * 소셜 추천을 먼저 시도하고, 비면 인기 메뉴로 폴백한다.
     *
     * 소셜 추천이 비는 경우는 두 가지다.
     *   (1) 이웃이 없다      — 진짜 콜드스타트 (팔로우도 주문도 없는 신규 유저)
     *   (2) 필터에 다 걸렸다 — 이웃은 있는데 알러지·크레딧·이미먹음으로 전부 제외
     *
     * 어느 쪽이든 사용자에겐 빈 화면이므로 폴백은 같지만,
     * note 로 이유를 구분해서 내려준다.
     */
    fun recommendSocial(userId: Int, limit: Int): RecommendationResult {
        val social = orderGraphRepository.recommendSocial(userId, limit)
        if (social.isNotEmpty()) {
            return RecommendationResult(
                userId = userId,
                strategy = RecommendStrategy.SOCIAL,
                note = null,
                items = social,
            )
        }

        val reachable = orderGraphRepository.findSimilarUsers(userId, 1).isNotEmpty()
        return RecommendationResult(
            userId = userId,
            strategy = RecommendStrategy.POPULAR_FALLBACK,
            note = if (reachable) {
                "팔로우 기반 후보가 알러지·크레딧·이미 먹은 메뉴로 모두 걸러졌습니다."
            } else {
                "이웃이 없어 팔로우 기반 추천을 만들 수 없습니다 (콜드스타트)."
            },
            items = orderGraphRepository.recommendPopularFallback(userId, limit),
        )
    }

    fun findAllergyConflicts(): List<AllergyConflict> =
        orderGraphRepository.findAllergyConflicts()
}
