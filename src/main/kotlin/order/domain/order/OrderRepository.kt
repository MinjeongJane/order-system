package order.domain.order

import order.api.dto.OrderRequest

interface OrderRepository {
    fun saveOrder(request: OrderRequest): OrderHistory
    fun publishOrderEvent(id: Long, request: OrderRequest)
    fun findByUserId(userId: Long): List<OrderHistoryResult>
}