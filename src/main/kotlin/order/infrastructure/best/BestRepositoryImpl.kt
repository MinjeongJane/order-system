package order.infrastructure.best

import order.api.dto.OrderDetailsRequest
import order.domain.best.BestRepository
import org.springframework.stereotype.Repository

@Repository
class BestRepositoryImpl : BestRepository {
    override fun increaseOrderCountInRedis(orders: List<OrderDetailsRequest>): List<Long> {
        TODO("Not yet implemented")
    }

    override fun decreaseOrderCountInRedis(orders: List<OrderDetailsRequest>): List<Long> {
        TODO("Not yet implemented")
    }

    override fun recordMenuStatisticsBatch(request: List<OrderDetailsRequest>) {
        TODO("Not yet implemented")
    }
}