package order.domain.best

import order.api.dto.OrderDetailsRequest

interface BestRepository {

    fun increaseOrderCountInRedis(orders: List<OrderDetailsRequest>): List<Long>

    fun decreaseOrderCountInRedis(orders: List<OrderDetailsRequest>): List<Long>

    fun recordMenuStatisticsBatch(request: List<OrderDetailsRequest>)
}