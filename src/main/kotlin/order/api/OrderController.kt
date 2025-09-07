package order.api

import jakarta.validation.Valid
import order.api.dto.BestMenuResponse
import order.api.dto.CreditChargeRequest
import order.api.dto.MenuResponse
import order.api.dto.OrderRequest
import order.api.dto.Response
import order.application.best.BestService
import order.application.menu.MenuService
import order.application.order.OrderService
import order.application.user.UserCreditService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/order")
class OrderController(
    private val menuService: MenuService,
    private val creditService: UserCreditService,
    private val orderService: OrderService,
    private val bestService: BestService,
) {
    // 메뉴 조회
    @GetMapping("/menu")
    fun findMenu(): Response<List<MenuResponse>> {
        val menu = menuService.findMenuAll()
        return Response.ok(
            menu.map { MenuResponse(id = it.id, name = it.name, price = it.price) }
        )
    }

    // 크레딧 충전
    @PostMapping("/charge")
    fun chargeCredit(@RequestBody @Valid request: CreditChargeRequest): Response<String> {
        creditService.charge(userId = request.userId, credits = request.credits)
        return Response.ok(CHARGE_SUCCESS_MESSAGE)
    }

    // 아이스크림 주문
    @PostMapping
    fun order(@RequestBody @Valid request: OrderRequest): Response<String> {
        val result = orderService.order(request)
        return Response.ok("주문이 완료되었습니다. 사용자 ID: ${result.userId}, 총 결제 가격: ${result.price}")
    }

    // 인기메뉴 조회
    @GetMapping("/best-menu")
    fun findBestMenu(): Response<Any> {
        val bestMenus = bestService.findBestMenu()
        if (bestMenus.isEmpty()) return Response.ok(EMPTY_STATISTICS)

        val menus = menuService.findMenuByIds(bestMenus.map { it.menuId })
        val result = menus.zip(bestMenus) { menu, count ->
            BestMenuResponse(menuId = menu.id.toInt(), name = menu.name, orderCount = count.orderCount)
        }
        return Response.ok(result)
    }

    companion object {
        private const val CHARGE_SUCCESS_MESSAGE = "크레딧 충전이 완료되었습니다."
        private const val EMPTY_STATISTICS = "집계 된 메뉴가 없습니다."
    }
}