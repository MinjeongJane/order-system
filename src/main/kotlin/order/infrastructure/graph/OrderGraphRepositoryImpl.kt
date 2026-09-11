package order.infrastructure.graph

import order.domain.graph.AllergyConflict
import order.domain.graph.CoOrderedMenu
import order.domain.graph.GraphRecommendation
import order.domain.graph.MenuPopularity
import order.domain.graph.OrderGraphRepository
import order.domain.graph.SimilarUser
import order.domain.graph.SocialRecommendation
import order.domain.graph.UserConnection
import order.domain.graph.UserMenuTaste
import org.neo4j.driver.Driver
import org.neo4j.driver.Record
import org.neo4j.driver.SessionConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

@Repository
class OrderGraphRepositoryImpl(
    private val driver: Driver,
    @Value("\${neo4j.database}") private val database: String,
) : OrderGraphRepository {

    override fun findUserTaste(userId: Int): List<UserMenuTaste> =
        read(CypherQueries.USER_TASTE, mapOf("userId" to userId)) {
            UserMenuTaste(
                menuId = it["menuId"].asInt(),
                menuName = it["menuName"].asString(),
                orderTimes = it["orderTimes"].asInt(),
                totalCount = it["totalCount"].asInt(),
            )
        }

    override fun findCoOrderedMenus(limit: Int): List<CoOrderedMenu> =
        read(CypherQueries.CO_ORDERED_MENUS, mapOf("limit" to limit)) {
            CoOrderedMenu(
                menuId = it["menuId"].asInt(),
                menuName = it["menuName"].asString(),
                pairedMenuId = it["pairedMenuId"].asInt(),
                pairedMenuName = it["pairedMenuName"].asString(),
                weight = it["weight"].asInt(),
            )
        }

    override fun findSimilarUsers(userId: Int, limit: Int): List<SimilarUser> =
        read(CypherQueries.SIMILAR_USERS, mapOf("userId" to userId, "limit" to limit)) {
            SimilarUser(
                userId = it["userId"].asInt(),
                sharedMenuCount = it["sharedMenuCount"].asInt(),
                sharedMenuNames = it["sharedMenuNames"].asList { v -> v.asString() },
            )
        }

    override fun recommendMenus(userId: Int, limit: Int): List<GraphRecommendation> =
        read(CypherQueries.RECOMMEND_MENUS, mapOf("userId" to userId, "limit" to limit)) {
            GraphRecommendation(
                menuId = it["menuId"].asInt(),
                menuName = it["menuName"].asString(),
                price = it["price"].asInt(),
                score = it["score"].asInt(),
                recommendedBy = it["recommendedBy"].asList { v -> v.asInt() },
            )
        }

    override fun findConnection(fromUserId: Int, toUserId: Int): UserConnection? =
        read(
            CypherQueries.SHORTEST_PATH,
            mapOf("fromUserId" to fromUserId, "toUserId" to toUserId),
        ) {
            UserConnection(
                fromUserId = fromUserId,
                toUserId = toUserId,
                hops = it["hops"].asInt(),
                path = it["path"].asList { v -> v.asString() },
            )
        }.firstOrNull()

    override fun findMenuPopularity(limit: Int): List<MenuPopularity> =
        read(CypherQueries.MENU_POPULARITY, mapOf("limit" to limit)) {
            MenuPopularity(
                menuId = it["menuId"].asInt(),
                menuName = it["menuName"].asString(),
                distinctUsers = it["distinctUsers"].asInt(),
                orderCount = it["orderCount"].asInt(),
                totalQuantity = it["totalQuantity"].asInt(),
            )
        }

    override fun recommendSocial(userId: Int, limit: Int): List<SocialRecommendation> =
        read(CypherQueries.SOCIAL_RECOMMEND, mapOf("userId" to userId, "limit" to limit)) {
            SocialRecommendation(
                menuId = it["menuId"].asInt(),
                menuName = it["menuName"].asString(),
                price = it["price"].asInt(),
                score = it["score"].asInt(),
                likedBy = it["likedBy"].asList { v -> v.asString() },
            )
        }

    override fun recommendPopularFallback(userId: Int, limit: Int): List<SocialRecommendation> =
        read(CypherQueries.POPULAR_FALLBACK, mapOf("userId" to userId, "limit" to limit)) {
            SocialRecommendation(
                menuId = it["menuId"].asInt(),
                menuName = it["menuName"].asString(),
                price = it["price"].asInt(),
                score = it["score"].asInt(),
                likedBy = emptyList(),
                affordable = it["affordable"].asBoolean(),
            )
        }

    override fun findAllergyConflicts(): List<AllergyConflict> =
        read(CypherQueries.ALLERGY_CONFLICTS, emptyMap()) {
            AllergyConflict(
                userId = it["userId"].asInt(),
                userName = it["userName"].asString(),
                menuId = it["menuId"].asInt(),
                menuName = it["menuName"].asString(),
                causeIngredient = it["causeIngredient"].asString(),
                allergyName = it["allergyName"].asString(),
                orderedAt = it["orderedAt"].asString(),
                registeredAt = it["registeredAt"].asString(),
                afterRegistration = it["afterRegistration"].asBoolean(),
            )
        }

    private fun <T> read(cypher: String, params: Map<String, Any>, mapper: (Record) -> T): List<T> =
        driver.session(SessionConfig.forDatabase(database)).use { session ->
            session.executeRead { tx -> tx.run(cypher, params).list(mapper) }
        }
}
