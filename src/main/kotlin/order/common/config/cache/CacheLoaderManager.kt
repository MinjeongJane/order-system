package order.common.config.cache

import order.application.menu.MenuService
import order.domain.menu.Menu
import org.springframework.stereotype.Component

@Component
class CacheLoaderManager(
    private val menuService: MenuService,
) {
    fun loadMenusCache(menuIds: List<Int>): List<Menu> {
        return menuService.findMenuByIds(menuIds)
    }
}