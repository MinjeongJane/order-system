package order.domain.menu

interface MenuRepository {
    fun findMenuAll(): List<Menu>

    fun existsByIds(menuIds: List<Int>): Boolean

    fun findMenuByIds(menuIds: List<Int>): List<Menu>
}