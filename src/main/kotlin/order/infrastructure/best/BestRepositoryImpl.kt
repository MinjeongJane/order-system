package order.infrastructure.best

import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.Duration
import java.time.LocalDate
import order.api.dto.OrderDetailsRequest
import order.domain.best.BestMenu
import order.domain.best.BestRepository
import order.domain.best.MenuOrderStatics
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Repository

@Repository
class BestRepositoryImpl(
    private val redissonClient: RedissonClient,
    private val bestJpaRepository: BestJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : BestRepository {
    override fun findBestMenuInRedis(): List<BestMenu> {
        val map = redissonClient.getMap<String, String>(BEST_MENU_KEY)

        return map.readAllMap()
            .entries
            .sortedByDescending { it.value.toLong() }
            .take(BEST_MENU_LIMIT)
            .map { BestMenu(it.key.toInt(), it.value.toInt()) }
    }

    override fun getOrCacheBestMenu(): List<BestMenu> {
        val cached = findBestMenuInRedis()
        if (cached.isNotEmpty()) return cached

        val dbResult = findBestMenuInDB()
        if (dbResult.isNotEmpty()) {
            cacheBestMenuToRedis(dbResult) // 집계 결과를 Redis에 저장
        }
        return dbResult
    }

    private fun cacheBestMenuToRedis(menuIdCounts: List<BestMenu>) {
        val map = redissonClient.getMap<String, Long>(BEST_MENU_KEY)
        val data = menuIdCounts.associate { it.menuId.toString() to it.orderCount.toLong() }
        map.putAll(data)
        map.expire(Duration.ofDays(REDIS_EXPIRED_DAYS))
    }

    override fun findBestMenuInDB(): List<BestMenu> {
        val qMenuOrder = QMenuOrderStatisticsEntity.menuOrderStatisticsEntity
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(7)

        return queryFactory
            .select(qMenuOrder.menuId, qMenuOrder.count.sum())
            .from(qMenuOrder)
            .where(qMenuOrder.date.between(startDate, endDate))
            .groupBy(qMenuOrder.menuId)
            .orderBy(qMenuOrder.count.sum().desc())
            .limit(BEST_MENU_LIMIT.toLong())
            .fetch()
            .mapNotNull { tuple ->
                val menuId = tuple.get(qMenuOrder.menuId)
                val count = tuple.get(qMenuOrder.count.sum())
                if (menuId != null && count != null) BestMenu(menuId, count) else null
            }
    }

    //    @Async 데이터 정합성이 더 중요하다면, 비동기 처리
    override fun increaseOrderCountInRedis(orders: List<OrderDetailsRequest>): List<Long> =
        redissonClient.getMap<String, Long>(BEST_MENU_KEY).let { map ->
            map.expireIfNotSet(Duration.ofDays(REDIS_EXPIRED_DAYS))
            orders.map { order ->
                map.addAndGet(order.menuId.toString(), order.count.toLong())
            }
        }

    //    @Async
    override fun decreaseOrderCountInRedis(orders: List<OrderDetailsRequest>): List<Long> {
        val map = redissonClient.getMap<String, Long>(BEST_MENU_KEY)
        return orders.map { order ->
            val updatedCount = map.addAndGet(order.menuId.toString(), -order.count.toLong())
            if (updatedCount <= 0) {
                map.remove(order.menuId.toString())
            }
            updatedCount
        }
    }

    override fun findMenuByDate(today: LocalDate, menuIds: List<Int>): List<MenuOrderStatics> =
        bestJpaRepository.findByDateAndMenuIdIn(today, menuIds)
            .map { it.toMenuOrderStatics() }

    override fun recordMenuStatisticsBatch(request: List<OrderDetailsRequest>) {
        val today = LocalDate.now()
        val existingStats = bestJpaRepository
            .findByDateAndMenuIdIn(today, request.map { it.menuId })
            .associateBy { it.menuId!! } // menuId 기준으로 맵핑

        val toSave = request.map { order ->
            existingStats[order.menuId]?.apply { increaseCount(order.count) }
                ?: MenuOrderStatisticsEntity(date = today, menuId = order.menuId, count = order.count)
        }

        bestJpaRepository.saveAll(toSave)
    }

    companion object {
        const val BEST_MENU_KEY = "best_menu"
        const val BEST_MENU_LIMIT = 3
        const val REDIS_EXPIRED_DAYS = 7L
    }
}