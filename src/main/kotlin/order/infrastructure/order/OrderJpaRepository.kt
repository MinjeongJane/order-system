package order.infrastructure.order

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrderJpaRepository : JpaRepository<OrderHistoryEntity, Long> {
    @Query("SELECT oh FROM OrderHistoryEntity oh WHERE oh.userId = :userId AND oh.deleted = false")
    fun findByUserIdAndNotDeleted(@Param("userId") userId: Long, pageable: Pageable): Page<OrderHistoryEntity>
}