package order.infrastructure.menu

import order.api.dto.MenuRequest
import order.domain.menu.Menu
import order.domain.menu.MenuRepository
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.stereotype.Repository

@Repository
class MenuRepositoryImpl(
    private val menuJpaRepository: MenuJpaRepository
) : MenuRepository {
    override fun findMenuAll(): List<Menu> = menuJpaRepository.findAll().map { it.toMenu() }

    override fun existsByIds(menuIds: List<Int>): Boolean =
        menuJpaRepository.findAllById(menuIds.map { it.toLong() }).size == menuIds.size

    override fun findMenuByIds(menuIds: List<Int>): List<Menu> =
        menuJpaRepository.findByIdIn(menuIds.map { it.toLong() }).map { it.toMenu() }

    override fun saveMenu(menus: List<MenuRequest>) {
        val entities = menus.map { MenuEntity(id = it.id, name = it.name, price = it.price) }
        val result = menuJpaRepository.saveAll(entities)
        if (result.isEmpty()) throw DataAccessResourceFailureException("메뉴 저장에 실패했습니다.")
    }
}