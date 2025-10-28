package order.domain.menu

import order.api.dto.MenuRequest

interface MenuRepository {
    fun findMenuAll(): List<Menu>

    fun existsByIds(menuIds: List<Int>): Boolean

    fun findMenuByIds(menuIds: List<Int>): List<Menu>

    fun saveMenu(menus: List<MenuRequest>)
}