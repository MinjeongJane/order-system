package order.common.config

import io.github.resilience4j.ratelimiter.RateLimiter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest(classes = [Resilience4jConfig::class])
class Resilience4jConfigTest {

    @Autowired
    lateinit var dbFallbackLimiter: RateLimiter

    @Test
    fun `dbFallbackLimiter 빈이 정상적으로 생성된다`() {
        assertNotNull(dbFallbackLimiter)
        assertEquals("dbFallbackLimiter", dbFallbackLimiter.name)
    }
}