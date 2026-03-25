package order.infrastructure.order

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrderJpaRepository : JpaRepository<OrderHistoryEntity, Long> {

    @Query("""
        SELECT DISTINCT oh FROM OrderHistoryEntity oh
        LEFT JOIN FETCH oh.orderDetails
        WHERE oh.userId = :userId AND oh.deleted = false
        ORDER BY oh.id DESC
    """)
    fun findAllByUserIdWithDetails(@Param("userId") userId: Long): List<OrderHistoryEntity>
}