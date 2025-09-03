package order.infrastructure.best

import java.time.LocalDate
import org.springframework.data.jpa.repository.JpaRepository

interface BestJpaRepository : JpaRepository<MenuOrderStatisticsEntity, Long> {
    fun findByDateAndMenuIdIn(date: LocalDate, menuIds: List<Int>): List<MenuOrderStatisticsEntity>
    fun deleteByMenuId(menuId: Int)
}