package order.domain.order

import order.api.dto.OrderRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface OrderRepository {
    fun saveOrder(request: OrderRequest): OrderHistory
    fun publishOrderEvent(id: Long, request: OrderRequest)
    fun findOrdersByUserId(userId: Long, pageable: Pageable): Page<OrderHistory>
}