package order.infrastructure.menu

import org.springframework.data.jpa.repository.JpaRepository

interface MenuJpaRepository : JpaRepository<MenuEntity, Long> {
    fun findByIdIn(menuIds: List<Long>): List<MenuEntity>
}