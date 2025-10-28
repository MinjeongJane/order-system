package order.application.admin

import order.api.dto.MenuRequest
import order.domain.menu.MenuRepository
import org.springframework.stereotype.Service

@Service
class AdminService(
    private val menuRepository: MenuRepository,
) {
    fun saveMenu(menus: List<MenuRequest>) {
        menuRepository.saveMenu(menus)
    }
}
