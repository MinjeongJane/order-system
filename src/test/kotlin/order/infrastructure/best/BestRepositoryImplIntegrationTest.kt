package order.infrastructure.best

import TestJpaConfig
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.stream.IntStream
import order.api.dto.OrderDetailsRequest
import order.common.config.JacksonConfig
import order.domain.best.BestRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@Import(JacksonConfig::class, TestJpaConfig::class)
@SpringBootTest(properties = ["spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"])
class BestRepositoryImplIntegrationTest @Autowired constructor(
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
            assertTrue(bestMenus.any { it.menuId == MENU_ID_10 && it.orderCount == MENU_COUNT_10 })
            assertTrue(bestMenus.any { it.menuId == MENU_ID_20 && it.orderCount == MENU_COUNT_20 })
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

    @Test
    fun `increaseOrderCountInRedis 동시성 테스트, CountDownLatch 사용`() {
        val menuId = 100
        val threadCount = 10
        val orderCountPerThread = 5
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        // redis 초기화
        val map = redissonClient.getMap<String, String>("best_menu")
        map.remove(menuId.toString())

        repeat(threadCount) {
            executor.submit {
                try {
                    bestRepository.increaseOrderCountInRedis(
                        listOf(OrderDetailsRequest(menuId, orderCountPerThread, 1000))
                    )
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        val finalCount = map[menuId.toString()]?.toLong() ?: 0L
        assertEquals((threadCount * orderCountPerThread).toLong(), finalCount)

        // 원복
        map.remove(menuId.toString())
    }

    @Test
    fun `decreaseOrderCountInRedis 동시성 테스트, IntStream parallel 사용`() {
        val menuId = 200
        val initialCount = 20
        val decreasePerThread = 5
        val threadCount = 4

        // redis 초기화
        val map = redissonClient.getMap<String, String>("best_menu")
        map.put(menuId.toString(), initialCount.toString())

        IntStream.range(0, threadCount)
            .parallel()
            .forEach {
                bestRepository.decreaseOrderCountInRedis(
                    listOf(OrderDetailsRequest(menuId, decreasePerThread, 1000))
                )
            }

        val finalCount = map[menuId.toString()]?.toLong() ?: 0L
        assertTrue(finalCount <= 0L)

        // 원복
        map.remove(menuId.toString())
    }
}