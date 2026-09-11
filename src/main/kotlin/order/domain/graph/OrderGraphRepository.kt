package order.domain.graph

interface OrderGraphRepository {
    /** 유저가 주문한 메뉴 취향 집계 */
    fun findUserTaste(userId: Int): List<UserMenuTaste>

    /** 함께 주문된 메뉴 쌍 */
    fun findCoOrderedMenus(limit: Int): List<CoOrderedMenu>

    /** 취향이 겹치는 유저 */
    fun findSimilarUsers(userId: Int, limit: Int): List<SimilarUser>

    /** 협업 필터링 기반 메뉴 추천 */
    fun recommendMenus(userId: Int, limit: Int): List<GraphRecommendation>

    /** 두 유저를 잇는 최단 경로 */
    fun findConnection(fromUserId: Int, toUserId: Int): UserConnection?

    /** 그래프 연결 차수 기반 인기 메뉴 */
    fun findMenuPopularity(limit: Int): List<MenuPopularity>

    /** §7 팔로우 + 알러지 + 크레딧을 결합한 소셜 추천 */
    fun recommendSocial(userId: Int, limit: Int): List<SocialRecommendation>

    /** §7 콜드스타트 폴백 — 인기 메뉴 (알러지·이미먹음은 제외, 크레딧은 플래그로만) */
    fun recommendPopularFallback(userId: Int, limit: Int): List<SocialRecommendation>

    /** §7 알러지 성분이 든 메뉴를 주문한 이력 (등록 시점 판정 포함) */
    fun findAllergyConflicts(): List<AllergyConflict>
}
