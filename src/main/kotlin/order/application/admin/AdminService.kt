package order.application.admin

import order.api.dto.MenuRequest
import order.domain.menu.MenuRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val menuRepository: MenuRepository,
) {
    @Transactional
    fun saveMenu(menus: List<MenuRequest>) {
        menuRepository.saveMenu(menus)
    }
}
