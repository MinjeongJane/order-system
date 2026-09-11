package order.infrastructure.graph

/**
 * 실습용 Cypher 모음.
 *
 * 그래프 모델
 *   (:User  {id, credits})
 *   (:Menu  {id, name, price})
 *   (:Order {id, price, orderedAt})
 *
 *   (:User)  -[:PLACED]->      (:Order)
 *   (:Order) -[:CONTAINS {count, menuPrice}]-> (:Menu)
 *   (:Menu)  -[:CO_ORDERED {weight}]->         (:Menu)   // 파생 관계
 *
 * §7 확장 (성분 · 알러지 · 팔로우 · 카테고리)
 *   (:User)-[:FOLLOWS]->(:User)                       // 방향 있음
 *   (:User)-[:LIKES {rating}]->(:Menu)                // 명시적 선호
 *   (:User)-[:ALLERGIC_TO {registeredAt}]->(:Ingredient)
 *   (:Menu)-[:MADE_WITH]->(:Ingredient)               // MENU.ingredients JSON 을 펼친 것
 *   (:Ingredient)-[:PART_OF]->(:Ingredient)           // 성분 분류 트리
 *   (:Menu)-[:IN_CATEGORY]->(:Category)
 *   (:Category)-[:SUBCATEGORY_OF]->(:Category)
 */
object CypherQueries {

    /* ------------------------------------------------------------------ 스키마 */

    val CONSTRAINTS = listOf(
        "CREATE CONSTRAINT user_id_unique  IF NOT EXISTS FOR (u:User)  REQUIRE u.id IS UNIQUE",
        "CREATE CONSTRAINT menu_id_unique  IF NOT EXISTS FOR (m:Menu)  REQUIRE m.id IS UNIQUE",
        "CREATE CONSTRAINT order_id_unique IF NOT EXISTS FOR (o:Order) REQUIRE o.id IS UNIQUE",
        "CREATE CONSTRAINT ingredient_id_unique IF NOT EXISTS FOR (i:Ingredient) REQUIRE i.id IS UNIQUE",
        "CREATE CONSTRAINT category_id_unique   IF NOT EXISTS FOR (c:Category)   REQUIRE c.id IS UNIQUE",
    )

    /* -------------------------------------------------------------------- 적재 */

    const val CLEAR_GRAPH = "MATCH (n) DETACH DELETE n"

    const val MERGE_USERS = """
        UNWIND ${'$'}rows AS row
        MERGE (u:User {id: row.id})
        SET u.credits = row.credits
    """

    const val MERGE_MENUS = """
        UNWIND ${'$'}rows AS row
        MERGE (m:Menu {id: row.id})
        SET m.name = row.name, m.price = row.price
    """

    /* ------------------------------------------------- §7 확장 적재 */

    /** USER 테이블의 이름을 (:User) 에 붙인다. USER_CREDIT 과 id 를 공유한다. */
    const val MERGE_USER_NAMES = """
        UNWIND ${'$'}rows AS row
        MERGE (u:User {id: row.id})
        SET u.name = row.name
    """

    /** 성분 분류 트리. parent 가 있으면 (:Ingredient)-[:PART_OF]->(:Ingredient) */
    const val MERGE_INGREDIENTS = """
        UNWIND ${'$'}rows AS row
        MERGE (i:Ingredient {id: row.id})
        SET i.name = row.name
    """

    const val MERGE_INGREDIENT_TREE = """
        UNWIND ${'$'}rows AS row
        MATCH (c:Ingredient {id: row.id})
        MATCH (p:Ingredient {id: row.parentId})
        MERGE (c)-[:PART_OF]->(p)
    """

    const val MERGE_CATEGORIES = """
        UNWIND ${'$'}rows AS row
        MERGE (c:Category {id: row.id})
        SET c.name = row.name
    """

    const val MERGE_CATEGORY_TREE = """
        UNWIND ${'$'}rows AS row
        MATCH (c:Category {id: row.id})
        MATCH (p:Category {id: row.parentId})
        MERGE (c)-[:SUBCATEGORY_OF]->(p)
    """

    const val MERGE_MENU_CATEGORY = """
        UNWIND ${'$'}rows AS row
        MATCH (m:Menu {id: row.menuId})
        MATCH (c:Category {id: row.categoryId})
        MERGE (m)-[:IN_CATEGORY]->(c)
    """

    /**
     * MENU.ingredients (JSON 배열) 를 펼쳐 만든 관계.
     * MySQL 에서 JSON_TABLE 로 이미 행 단위로 펼쳐서 넘어온다.
     * 비정규화(문자열 배열) -> 정규화(노드+관계) 가 바로 이 지점이다.
     */
    const val MERGE_MENU_INGREDIENTS = """
        UNWIND ${'$'}rows AS row
        MATCH (m:Menu {id: row.menuId})
        MATCH (i:Ingredient {name: row.ingredientName})
        MERGE (m)-[:MADE_WITH]->(i)
    """

    const val MERGE_USER_ALLERGIES = """
        UNWIND ${'$'}rows AS row
        MATCH (u:User {id: row.userId})
        MATCH (i:Ingredient {id: row.ingredientId})
        MERGE (u)-[a:ALLERGIC_TO]->(i)
        SET a.registeredAt = row.registeredAt
    """

    /** 팔로우는 방향이 있다. 맞팔이면 두 행이 각각 들어온다. */
    const val MERGE_FOLLOWS = """
        UNWIND ${'$'}rows AS row
        MATCH (f:User {id: row.followerId})
        MATCH (t:User {id: row.followeeId})
        MERGE (f)-[:FOLLOWS]->(t)
    """

    const val MERGE_LIKES = """
        UNWIND ${'$'}rows AS row
        MATCH (u:User {id: row.userId})
        MATCH (m:Menu {id: row.menuId})
        MERGE (u)-[l:LIKES]->(m)
        SET l.rating = row.rating
    """

    const val MERGE_ORDERS = """
        UNWIND ${'$'}rows AS row
        MATCH (u:User {id: row.userId})
        MERGE (o:Order {id: row.id})
        SET o.price = row.price, o.orderedAt = row.orderedAt
        MERGE (u)-[:PLACED]->(o)
    """

    const val MERGE_ORDER_DETAILS = """
        UNWIND ${'$'}rows AS row
        MATCH (o:Order {id: row.orderId})
        MATCH (m:Menu  {id: row.menuId})
        MERGE (o)-[c:CONTAINS]->(m)
        SET c.count = row.count, c.menuPrice = row.menuPrice
    """

    /**
     * 파생 관계: 같은 주문에 함께 담긴 메뉴를 하나의 엣지로 요약한다.
     * m1.id < m2.id 조건으로 (A,B)/(B,A) 중복을 막고, 조회할 때 방향 없이 탐색한다.
     */
    const val BUILD_CO_ORDERED = """
        MATCH (m1:Menu)<-[:CONTAINS]-(o:Order)-[:CONTAINS]->(m2:Menu)
        WHERE m1.id < m2.id
        WITH m1, m2, count(DISTINCT o) AS weight
        MERGE (m1)-[r:CO_ORDERED]->(m2)
        SET r.weight = weight
        RETURN count(r) AS created
    """

    /* -------------------------------------------------------------------- 조회 */

    /** 1) 유저 취향: 2-hop 을 집계로 압축 */
    const val USER_TASTE = """
        MATCH (u:User {id: ${'$'}userId})-[:PLACED]->(o:Order)-[c:CONTAINS]->(m:Menu)
        RETURN m.id            AS menuId,
               m.name          AS menuName,
               count(DISTINCT o) AS orderTimes,
               sum(c.count)    AS totalCount
        ORDER BY totalCount DESC, menuName
    """

    /** 2) 함께 주문된 메뉴 (장바구니 연관 분석) */
    const val CO_ORDERED_MENUS = """
        MATCH (m1:Menu)-[r:CO_ORDERED]->(m2:Menu)
        RETURN m1.id   AS menuId,
               m1.name AS menuName,
               m2.id   AS pairedMenuId,
               m2.name AS pairedMenuName,
               r.weight AS weight
        ORDER BY weight DESC, menuName
        LIMIT ${'$'}limit
    """

    /** 3) 취향이 겹치는 유저: User -> Order -> Menu <- Order <- User (4-hop) */
    const val SIMILAR_USERS = """
        MATCH (me:User {id: ${'$'}userId})-[:PLACED]->(:Order)-[:CONTAINS]->(m:Menu)
              <-[:CONTAINS]-(:Order)<-[:PLACED]-(other:User)
        WHERE other.id <> me.id
        WITH other, collect(DISTINCT m.name) AS shared
        RETURN other.id  AS userId,
               size(shared) AS sharedMenuCount,
               shared     AS sharedMenuNames
        ORDER BY sharedMenuCount DESC, userId
        LIMIT ${'$'}limit
    """

    /**
     * 4) 협업 필터링 추천
     *    "나와 같은 메뉴를 먹은 사람들이 먹었지만, 나는 아직 안 먹은 메뉴"
     *    SQL 이면 self-join 3번 + NOT EXISTS 가 필요한 질의가 경로 하나로 표현된다.
     */
    const val RECOMMEND_MENUS = """
        MATCH (me:User {id: ${'$'}userId})-[:PLACED]->(:Order)-[:CONTAINS]->(mine:Menu)
        WITH me, collect(DISTINCT mine) AS myMenus
        MATCH (me)-[:PLACED]->(:Order)-[:CONTAINS]->(:Menu)
              <-[:CONTAINS]-(:Order)<-[:PLACED]-(other:User)
        WHERE other <> me
        MATCH (other)-[:PLACED]->(:Order)-[:CONTAINS]->(candidate:Menu)
        WHERE NOT candidate IN myMenus
        RETURN candidate.id    AS menuId,
               candidate.name  AS menuName,
               candidate.price AS price,
               count(DISTINCT other)      AS score,
               collect(DISTINCT other.id) AS recommendedBy
        ORDER BY score DESC, menuName
        LIMIT ${'$'}limit
    """

    /** 5) 두 유저를 잇는 최단 경로 — 관계형 DB 로는 표현 자체가 어려운 질의 */
    const val SHORTEST_PATH = """
        MATCH (a:User {id: ${'$'}fromUserId}), (b:User {id: ${'$'}toUserId})
        MATCH p = shortestPath((a)-[:PLACED|CONTAINS*..10]-(b))
        RETURN length(p) AS hops,
               [n IN nodes(p) | head(labels(n)) + ':' + coalesce(n.name, toString(n.id))] AS path
    """

    /**
     * 7) ★ 하이라이트 — 팔로우 + 알러지 + 이미 먹어본 것 + 크레딧을 한 질의에
     *
     * "내가 팔로우하는 사람(과 그들이 팔로우하는 사람)이 좋아하는 메뉴 중,
     *  내 알러지 성분이 없고, 내가 아직 안 먹어봤고, 크레딧으로 살 수 있는 것"
     *
     * 가변 길이 탐색이 두 군데 중첩된다: FOLLOWS*1..2 (방향 있음), PART_OF*0.. (계층).
     * SQL 이면 재귀 CTE + JSON_TABLE + NOT EXISTS + 조인 예닐곱 개가 필요하다.
     */
    const val SOCIAL_RECOMMEND = """
        MATCH (me:User {id: ${'$'}userId})-[:FOLLOWS*1..2]->(other:User)
        WHERE other <> me
        MATCH (other)-[:LIKES]->(candidate:Menu)
        WHERE NOT EXISTS { (me)-[:PLACED]->(:Order)-[:CONTAINS]->(candidate) }
          AND NOT EXISTS {
                MATCH (me)-[:ALLERGIC_TO]->(mine:Ingredient)
                MATCH (candidate)-[:MADE_WITH]->(:Ingredient)-[:PART_OF*0..]->(mine)
              }
          AND candidate.price <= me.credits
        RETURN candidate.id    AS menuId,
               candidate.name  AS menuName,
               candidate.price AS price,
               count(DISTINCT other)        AS score,
               collect(DISTINCT other.name) AS likedBy
        ORDER BY score DESC, menuName
        LIMIT ${'$'}limit
    """

    /**
     * 7-b) 콜드스타트 폴백 — 그래프 차수 기반 인기 메뉴.
     *
     * 소셜 추천이 빈 결과일 때 대신 내보낸다. 빈 이유는 두 가지다.
     *   (1) 이웃이 없다        — 진짜 콜드스타트 (팔로우 0, 주문 0)
     *   (2) 필터에 다 걸렸다   — 이웃은 있는데 알러지/크레딧/이미먹음으로 전부 제외
     *
     * **폴백에서도 알러지와 '이미 먹어본 것' 은 반드시 제외한다.**
     * 인기 있다고 알러지 있는 메뉴를 들이밀면 안 된다 — 안전 문제다.
     *
     * 반면 **크레딧은 필터하지 않고 affordable 플래그로만 표시**한다.
     * 잔액 부족은 결제 시점 문제지 "보여주면 안 되는" 이유가 아니다.
     * 추천에서 아예 지우면 사용자는 "왜 안 보이지?" 가 된다.
     */
    const val POPULAR_FALLBACK = """
        MATCH (me:User {id: ${'$'}userId})
        MATCH (u:User)-[:PLACED]->(:Order)-[:CONTAINS]->(candidate:Menu)
        WHERE u <> me
          AND NOT EXISTS { (me)-[:PLACED]->(:Order)-[:CONTAINS]->(candidate) }
          AND NOT EXISTS {
                MATCH (me)-[:ALLERGIC_TO]->(mine:Ingredient)
                MATCH (candidate)-[:MADE_WITH]->(:Ingredient)-[:PART_OF*0..]->(mine)
              }
        RETURN candidate.id    AS menuId,
               candidate.name  AS menuName,
               candidate.price AS price,
               count(DISTINCT u) AS score,
               [] AS likedBy,
               candidate.price <= me.credits AS affordable
        ORDER BY score DESC, menuName
        LIMIT ${'$'}limit
    """

    /**
     * 8) 알러지 성분이 든 메뉴를 주문한 이력.
     *
     * 시점 조건(o.orderedAt > r.registeredAt)을 넣고 빼는 것으로
     * "과거의 흔적" 과 "지금 경고할 일" 이 갈린다.
     * 알러지는 나중에 등록되므로 등록 이전 주문은 데이터 오류가 아니다.
     */
    const val ALLERGY_CONFLICTS = """
        MATCH (u:User)-[:PLACED]->(o:Order)-[:CONTAINS]->(m:Menu)
        MATCH (u)-[r:ALLERGIC_TO]->(mine:Ingredient)
        MATCH (m)-[:MADE_WITH]->(cause:Ingredient)-[:PART_OF*0..]->(mine)
        RETURN u.id        AS userId,
               u.name      AS userName,
               m.id        AS menuId,
               m.name      AS menuName,
               cause.name  AS causeIngredient,
               mine.name   AS allergyName,
               toString(o.orderedAt)     AS orderedAt,
               toString(r.registeredAt)  AS registeredAt,
               o.orderedAt > r.registeredAt AS afterRegistration
        ORDER BY userId, menuId
    """

    /** 6) 그래프 차수 기반 인기 메뉴 — Redis/MySQL 집계 결과와 비교해보기 */
    const val MENU_POPULARITY = """
        MATCH (u:User)-[:PLACED]->(o:Order)-[c:CONTAINS]->(m:Menu)
        RETURN m.id   AS menuId,
               m.name AS menuName,
               count(DISTINCT u) AS distinctUsers,
               count(DISTINCT o) AS orderCount,
               sum(c.count)      AS totalQuantity
        ORDER BY distinctUsers DESC, totalQuantity DESC
        LIMIT ${'$'}limit
    """
}
