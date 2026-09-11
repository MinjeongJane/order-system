package order.api

import order.api.dto.Response
import order.application.graph.OrderGraphService
import order.domain.graph.AllergyConflict
import order.domain.graph.CoOrderedMenu
import order.domain.graph.GraphRecommendation
import order.domain.graph.GraphSyncResult
import order.domain.graph.MenuPopularity
import order.domain.graph.SimilarUser
import order.domain.graph.RecommendationResult
import order.domain.graph.UserMenuTaste
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/graph")
class GraphController(
    private val orderGraphService: OrderGraphService,
) {
    // MySQL 주문 데이터를 Neo4j 그래프로 재적재
    @PostMapping("/sync")
    fun sync(): Response<GraphSyncResult> = Response.ok(orderGraphService.sync())

    // 유저가 주문한 메뉴 취향
    @GetMapping("/users/{userId}/taste")
    fun findUserTaste(@PathVariable userId: Int): Response<List<UserMenuTaste>> =
        Response.ok(orderGraphService.findUserTaste(userId))

    // 함께 주문된 메뉴 쌍
    @GetMapping("/co-ordered")
    fun findCoOrderedMenus(@RequestParam(defaultValue = "10") limit: Int): Response<List<CoOrderedMenu>> =
        Response.ok(orderGraphService.findCoOrderedMenus(limit))

    // 취향이 겹치는 유저
    @GetMapping("/users/{userId}/similar")
    fun findSimilarUsers(
        @PathVariable userId: Int,
        @RequestParam(defaultValue = "5") limit: Int,
    ): Response<List<SimilarUser>> = Response.ok(orderGraphService.findSimilarUsers(userId, limit))

    // 협업 필터링 기반 메뉴 추천
    @GetMapping("/users/{userId}/recommend")
    fun recommendMenus(
        @PathVariable userId: Int,
        @RequestParam(defaultValue = "5") limit: Int,
    ): Response<List<GraphRecommendation>> = Response.ok(orderGraphService.recommendMenus(userId, limit))

    // 두 유저를 잇는 최단 경로
    @GetMapping("/connection")
    fun findConnection(
        @RequestParam from: Int,
        @RequestParam to: Int,
    ): Response<Any> = orderGraphService.findConnection(from, to)
        ?.let { Response.ok<Any>(it) }
        ?: Response.ok(NO_CONNECTION)

    // 그래프 차수 기반 인기 메뉴
    @GetMapping("/users/{userId}/recommend/social")
    fun recommendSocial(
        @PathVariable userId: Int,
        @RequestParam(defaultValue = "10") limit: Int,
    ): Response<RecommendationResult> =
        Response.ok(orderGraphService.recommendSocial(userId, limit))

    @GetMapping("/allergy-conflicts")
    fun findAllergyConflicts(): Response<List<AllergyConflict>> =
        Response.ok(orderGraphService.findAllergyConflicts())

    @GetMapping("/popularity")
    fun findMenuPopularity(@RequestParam(defaultValue = "10") limit: Int): Response<List<MenuPopularity>> =
        Response.ok(orderGraphService.findMenuPopularity(limit))

    companion object {
        private const val NO_CONNECTION = "두 유저를 잇는 경로가 없습니다."
    }
}
