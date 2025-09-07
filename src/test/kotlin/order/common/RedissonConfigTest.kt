package order.common

import order.common.config.DistributedLockProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedissonConfigTest {

    @Autowired
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var distributedLockProperties: DistributedLockProperties

    @Test
    fun `RedissonClient 빈이 정상적으로 생성된다`() {
        assertNotNull(redissonClient)
    }

    @Test
    fun `DistributedLockProperties가 정상적으로 바인딩된다`() {
        assertNotNull(distributedLockProperties)
        assertEquals(5L, distributedLockProperties.defaultWaitTime)
        assertEquals(30L, distributedLockProperties.defaultLeaseTime)
        assertEquals(30000L, distributedLockProperties.watchdogTimeout)
        assertEquals("lock", distributedLockProperties.keyPrefix)
    }
}