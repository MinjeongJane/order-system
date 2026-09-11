package order.infrastructure.graph

import order.domain.graph.GraphSyncResult
import order.domain.graph.OrderGraphSyncRepository
import org.neo4j.driver.Driver
import org.neo4j.driver.SessionConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp

/**
 * MySQL(order_system) → Neo4j 전체 재적재.
 *
 * 실습 목적이라 데이터가 작으므로 한 번에 UNWIND 로 밀어넣는다.
 * 실제 서비스라면 Kafka 주문 이벤트를 구독해 증분 반영하는 쪽이 맞다.
 */
@Repository
class OrderGraphSyncRepositoryImpl(
    private val driver: Driver,
    private val jdbcTemplate: JdbcTemplate,
    @Value("\${neo4j.database}") private val database: String,
) : OrderGraphSyncRepository {

    override fun syncAll(): GraphSyncResult {
        val users = jdbcTemplate.queryForList(
            "SELECT id, credits FROM user_credit WHERE deleted = 0"
        ).map { mapOf("id" to it["id"], "credits" to it["credits"]) }

        val menus = jdbcTemplate.queryForList(
            "SELECT id, name, price FROM menu WHERE deleted = 0"
        ).map { mapOf("id" to it["id"], "name" to it["name"], "price" to it["price"]) }

        val orders = jdbcTemplate.queryForList(
            "SELECT id, user_id, price, created_at FROM order_history WHERE deleted = 0"
        ).map {
            mapOf(
                "id" to it["id"],
                "userId" to it["user_id"],
                "price" to it["price"],
                "orderedAt" to (it["created_at"] as Timestamp).toLocalDateTime(),
            )
        }

        val details = jdbcTemplate.queryForList(
            "SELECT order_id, menu_id, count, menu_price FROM order_details WHERE deleted = 0"
        ).map {
            mapOf(
                "orderId" to it["order_id"],
                "menuId" to it["menu_id"],
                "count" to it["count"],
                "menuPrice" to it["menu_price"],
            )
        }

        /* ------------------------------------------------- §7 확장 */

        val userNames = jdbcTemplate.queryForList(
            "SELECT id, name FROM `USER` WHERE deleted = 0"
        ).map { mapOf("id" to it["id"], "name" to it["name"]) }

        val ingredients = jdbcTemplate.queryForList(
            "SELECT id, name, parent_id FROM ingredient"
        ).map { mapOf("id" to it["id"], "name" to it["name"], "parentId" to it["parent_id"]) }
        val ingredientTree = ingredients.filter { it["parentId"] != null }

        val categories = jdbcTemplate.queryForList(
            "SELECT id, name, parent_id FROM menu_category"
        ).map { mapOf("id" to it["id"], "name" to it["name"], "parentId" to it["parent_id"]) }
        val categoryTree = categories.filter { it["parentId"] != null }

        val menuCategories = jdbcTemplate.queryForList(
            "SELECT id, category_id FROM menu WHERE deleted = 0 AND category_id IS NOT NULL"
        ).map { mapOf("menuId" to it["id"], "categoryId" to it["category_id"]) }

        // MENU.ingredients 는 JSON 배열이다. MySQL 에서 JSON_TABLE 로 행 단위로 펼쳐서 받는다.
        // 여기가 "비정규화(문자열 배열) -> 정규화(노드 + 관계)" 가 일어나는 지점이다.
        val menuIngredients = jdbcTemplate.queryForList(
            """
            SELECT m.id AS menu_id, jt.ing AS ingredient_name
            FROM menu m,
                 JSON_TABLE(m.ingredients, '${'$'}[*]' COLUMNS (ing VARCHAR(50) PATH '${'$'}')) jt
            WHERE m.deleted = 0 AND m.ingredients IS NOT NULL
            """
        ).map { mapOf("menuId" to it["menu_id"], "ingredientName" to it["ingredient_name"]) }

        val allergies = jdbcTemplate.queryForList(
            "SELECT user_id, ingredient_id, registered_at FROM user_allergy"
        ).map {
            mapOf(
                "userId" to it["user_id"],
                "ingredientId" to it["ingredient_id"],
                "registeredAt" to (it["registered_at"] as Timestamp).toLocalDateTime(),
            )
        }

        val follows = jdbcTemplate.queryForList(
            "SELECT follower_id, followee_id FROM user_relation"
        ).map { mapOf("followerId" to it["follower_id"], "followeeId" to it["followee_id"]) }

        val likes = jdbcTemplate.queryForList(
            "SELECT user_id, menu_id, rating FROM user_menu_like"
        ).map { mapOf("userId" to it["user_id"], "menuId" to it["menu_id"], "rating" to it["rating"]) }

        // 제약조건(스키마 변경)은 데이터 쓰기와 같은 트랜잭션에 섞을 수 없다.
        // 섞으면 "Tried to execute Write query after executing Schema modification" 로 실패한다.
        // 각 제약조건을 별도 트랜잭션으로 먼저 적용한다.
        driver.session(SessionConfig.forDatabase(database)).use { session ->
            CypherQueries.CONSTRAINTS.forEach { constraint ->
                session.executeWrite { tx -> tx.run(constraint).consume() }
            }
        }

        val coOrderedCount = driver.session(SessionConfig.forDatabase(database)).use { session ->
            session.executeWrite { tx ->
                tx.run(CypherQueries.CLEAR_GRAPH)
                tx.run(CypherQueries.MERGE_USERS, mapOf("rows" to users))
                tx.run(CypherQueries.MERGE_MENUS, mapOf("rows" to menus))
                tx.run(CypherQueries.MERGE_ORDERS, mapOf("rows" to orders))
                tx.run(CypherQueries.MERGE_ORDER_DETAILS, mapOf("rows" to details))

                // §7 확장. 노드를 모두 만든 뒤 관계를 건다 (트리는 부모가 먼저 있어야 한다).
                tx.run(CypherQueries.MERGE_USER_NAMES, mapOf("rows" to userNames))
                tx.run(CypherQueries.MERGE_INGREDIENTS, mapOf("rows" to ingredients))
                tx.run(CypherQueries.MERGE_INGREDIENT_TREE, mapOf("rows" to ingredientTree))
                tx.run(CypherQueries.MERGE_CATEGORIES, mapOf("rows" to categories))
                tx.run(CypherQueries.MERGE_CATEGORY_TREE, mapOf("rows" to categoryTree))
                tx.run(CypherQueries.MERGE_MENU_CATEGORY, mapOf("rows" to menuCategories))
                tx.run(CypherQueries.MERGE_MENU_INGREDIENTS, mapOf("rows" to menuIngredients))
                tx.run(CypherQueries.MERGE_USER_ALLERGIES, mapOf("rows" to allergies))
                tx.run(CypherQueries.MERGE_FOLLOWS, mapOf("rows" to follows))
                tx.run(CypherQueries.MERGE_LIKES, mapOf("rows" to likes))

                tx.run(CypherQueries.BUILD_CO_ORDERED).single()["created"].asInt()
            }
        }

        return GraphSyncResult(
            userCount = users.size,
            menuCount = menus.size,
            orderCount = orders.size,
            containsCount = details.size,
            coOrderedCount = coOrderedCount,
            ingredientCount = ingredients.size,
            madeWithCount = menuIngredients.size,
            categoryCount = categories.size,
            allergyCount = allergies.size,
            followCount = follows.size,
            likeCount = likes.size,
        )
    }
}
