package order.domain.best

import java.time.LocalDate
import order.api.dto.OrderDetailsRequest

interface BestRepository {
    fun findBestMenuInRedis(): List<Pair<Int, Int>>

    fun getOrCacheBestMenu(): List<Pair<Int, Int>>

    fun findBestMenuInDB(): List<Pair<Int, Int>>

    fun increaseOrderCountInRedis(orders: List<OrderDetailsRequest>): List<Long>

    fun decreaseOrderCountInRedis(orders: List<OrderDetailsRequest>): List<Long>

    fun findMenuByDate(today: LocalDate, menuIds: List<Int>): List<MenuOrderStatics>

    fun recordMenuStatisticsBatch(request: List<OrderDetailsRequest>)
}