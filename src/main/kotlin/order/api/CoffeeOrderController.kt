package order.api

import order.api.dto.MenuResponse
import order.api.dto.Response
import order.application.menu.MenuService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/order")
class CoffeeOrderController(
    private val menuService: MenuService,
) {
    // 메뉴 조회
    @GetMapping("/menu")
    fun findMenu(): Response<List<MenuResponse>> {
        val menu = menuService.findMenuAll()
        return Response.ok(
            menu.map { MenuResponse(id = it.id, name = it.name, price = it.price) }
        )
    }
}