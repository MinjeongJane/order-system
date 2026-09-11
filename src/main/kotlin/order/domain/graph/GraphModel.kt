package order.domain.graph

/**
 * 그래프 적재 결과
 */
data class GraphSyncResult(
    val userCount: Int,
    val menuCount: Int,
    val orderCount: Int,
    val containsCount: Int,
    val coOrderedCount: Int,
    // §7 확장
    val ingredientCount: Int = 0,
    val madeWithCount: Int = 0,
    val categoryCount: Int = 0,
    val allergyCount: Int = 0,
    val followCount: Int = 0,
    val likeCount: Int = 0,
)

/**
 * 유저가 실제로 주문한 메뉴 집계
 * (:User)-[:PLACED]->(:Order)-[:CONTAINS]->(:Menu) 2-hop 을 압축한 결과
 */
data class UserMenuTaste(
    val menuId: Int,
    val menuName: String,
    val orderTimes: Int,   // 몇 번의 주문에 등장했는지
    val totalCount: Int,   // 총 몇 개 담았는지
)

/**
 * 같은 주문 안에 함께 담긴 메뉴 쌍
 */
data class CoOrderedMenu(
    val menuId: Int,
    val menuName: String,
    val pairedMenuId: Int,
    val pairedMenuName: String,
    val weight: Int,       // 함께 담긴 주문 수
)

/**
 * 취향이 겹치는 유저
 */
data class SimilarUser(
    val userId: Int,
    val sharedMenuCount: Int,
    val sharedMenuNames: List<String>,
)

/**
 * 협업 필터링 추천 결과
 * "나와 같은 메뉴를 먹은 사람들이 먹었지만 나는 아직 안 먹은 메뉴"
 */
data class GraphRecommendation(
    val menuId: Int,
    val menuName: String,
    val price: Int,
    val score: Int,             // 추천한 이웃 유저 수 (가중치)
    val recommendedBy: List<Int>,
)

/**
 * 두 유저 사이를 잇는 최단 경로 (유저 → 주문 → 메뉴 → 주문 → 유저 …)
 */
data class UserConnection(
    val fromUserId: Int,
    val toUserId: Int,
    val hops: Int,
    val path: List<String>,     // 경로에 등장한 노드 라벨:식별자
)

/**
 * 그래프상 메뉴 인기도 (연결 차수 기반)
 */
data class MenuPopularity(
    val menuId: Int,
    val menuName: String,
    val distinctUsers: Int,
    val orderCount: Int,
    val totalQuantity: Int,
)


/**
 * §7 소셜 추천 결과
 * "내가 팔로우하는 사람(의 팔로우)이 좋아하는 메뉴 중,
 *  알러지 성분이 없고, 안 먹어봤고, 크레딧으로 살 수 있는 것"
 */
data class SocialRecommendation(
    val menuId: Int,
    val menuName: String,
    val price: Int,
    val score: Int,              // 그 메뉴를 좋아하는 2홉 이내 팔로잉 수 (폴백이면 주문한 유저 수)
    val likedBy: List<String>,
    val affordable: Boolean = true,  // 폴백에서만 false 가 나올 수 있다
)

/** 추천이 어떤 경로로 나왔는지 */
enum class RecommendStrategy {
    SOCIAL,             // 팔로우 기반 추천
    POPULAR_FALLBACK,   // 소셜 추천이 비어서 인기 메뉴로 대체
}

/**
 * 추천 결과 + 어떻게 나왔는지.
 *
 * 폴백했는지를 호출자가 알아야 UI 문구를 다르게 쓸 수 있다.
 * ("팔로우하는 분들이 좋아해요" vs "많이 찾는 메뉴예요")
 */
data class RecommendationResult(
    val userId: Int,
    val strategy: RecommendStrategy,
    val note: String?,
    val items: List<SocialRecommendation>,
)

/**
 * 알러지 성분이 든 메뉴를 주문한 이력.
 *
 * 알러지는 나중에 등록되므로 등록 이전 주문은 데이터 오류가 아니다.
 * afterRegistration = true 인 것만 실제로 경고 대상이다.
 */
data class AllergyConflict(
    val userId: Int,
    val userName: String,
    val menuId: Int,
    val menuName: String,
    val causeIngredient: String, // 트리를 타고 올라가 알러지에 닿은 실제 성분
    val allergyName: String,     // 유저가 등록한 알러지 노드
    val orderedAt: String,
    val registeredAt: String,
    val afterRegistration: Boolean,
)
