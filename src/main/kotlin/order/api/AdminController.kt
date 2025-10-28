package order.api

import order.api.dto.MenuRequest
import order.api.dto.Response
import order.application.admin.AdminService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val adminService: AdminService,
) {
    fun saveMenu(request: List<MenuRequest>): Response<String> {
        adminService.saveMenu(request)
        return Response.ok(MENU_SAVE_SUCCESS)
    }

    companion object {
        private const val MENU_SAVE_SUCCESS = "메뉴 저장이 완료되었습니다."
    }
}