package order.api

import jakarta.validation.Valid
import order.api.dto.CreditChargeRequest
import order.api.dto.MenuResponse
import order.api.dto.Response
import order.application.menu.MenuService
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

    companion object {
        private const val CHARGE_SUCCESS_MESSAGE = "크레딧 충전이 완료되었습니다."
    }
}