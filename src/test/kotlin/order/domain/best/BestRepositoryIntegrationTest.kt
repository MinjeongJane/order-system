package order.domain.best

import TestJpaConfig
import java.time.LocalDate
import order.api.dto.OrderDetailsRequest
import order.common.config.JacksonConfig
import order.infrastructure.best.BestJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Import(JacksonConfig::class, TestJpaConfig::class)
class BestRepositoryIntegrationTest @Autowired constructor(
    private val bestRepository: BestRepository,
    private val bestJpaRepository: BestJpaRepository,
    private val redissonClient: RedissonClient,
) {
    @Test
    fun `주문 카운트 증가 및 조회 - Redis`() {
        val MENU_ID_10 = 10
        val MENU_ID_20 = 20
        val MENU_COUNT_10 = 9999999
        val MENU_COUNT_20 = 9999998

        // given
        val orders = listOf(
            OrderDetailsRequest(menuId = MENU_ID_10, count = MENU_COUNT_10, menuPrice = 1000),
            OrderDetailsRequest(menuId = MENU_ID_20, count = MENU_COUNT_20, menuPrice = 2000)
        )

        try {
            // when
            bestRepository.increaseOrderCountInRedis(orders)
            val bestMenus = bestRepository.findBestMenuInRedis()

            // then
            assertTrue(bestMenus.any { it.first == MENU_ID_10 && it.second == MENU_COUNT_10 })
            assertTrue(bestMenus.any { it.first == MENU_ID_20 && it.second == MENU_COUNT_20 })
        } finally {
            // redis 기록 원복
            val map = redissonClient.getMap<String, Long>("best_menu")
            map.remove(MENU_ID_10.toString())
            map.remove(MENU_ID_20.toString())
        }
    }

    @Test
    @Transactional
    fun `메뉴 통계 배치 기록 및 조회`() {
        // given
        val today = LocalDate.now()
        val orders = listOf(
            OrderDetailsRequest(menuId = 10, count = 100, menuPrice = 1000)
        )
        bestRepository.recordMenuStatisticsBatch(orders)

        try {
            // when
            val stats = bestRepository.findMenuByDate(today, listOf(10))

            // then
            assertEquals(1, stats.size)
            assertEquals(10, stats[0].menuId)
            assertEquals(100, stats[0].count)
        } finally {
            // 배치 기록 원복
            bestJpaRepository.deleteByMenuId(10)
        }
    }
}