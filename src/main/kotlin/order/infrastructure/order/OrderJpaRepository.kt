package order.infrastructure.order

import org.springframework.data.jpa.repository.JpaRepository

interface OrderJpaRepository : JpaRepository<OrderHistoryEntity, Long>