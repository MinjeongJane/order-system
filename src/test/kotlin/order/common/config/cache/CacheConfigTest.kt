package order.common.config.cache

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager

@SpringBootTest
class CacheConfigTest {
    @Autowired
    lateinit var cacheManager: CacheManager

    @Test
    fun `Caffeine CacheManager 빈이 정상적으로 생성된다`() {
        assertNotNull(cacheManager)
        assertTrue(cacheManager.cacheNames.contains("menu"))
        assertTrue(cacheManager.cacheNames.contains("menus"))
    }
}