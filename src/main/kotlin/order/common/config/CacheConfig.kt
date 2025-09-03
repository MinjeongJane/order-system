package order.common.config

import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun caffeineCacheBuilder(): Caffeine<Any, Any> =
        Caffeine.newBuilder()
            .maximumSize(MAXIMUM_SIZE)
            .expireAfterWrite(EXPIRE_AFTER_WRITE, TimeUnit.MINUTES)
            .recordStats()

    @Primary
    @Bean
    fun cacheManager(caffeine: Caffeine<Any, Any>): CacheManager {
        val cacheManager = CaffeineCacheManager()
        cacheManager.cacheNames = listOf(MENU_CACHE, MENUS_CACHE)
        cacheManager.setCaffeine(caffeine)
        return cacheManager
    }

    @Bean
    fun redisCacheManager(
        redisConnectionFactory: RedisConnectionFactory
    ): RedisCacheManager {
        val config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))  // TTL 1시간 등 필요에 맞게 조절
            .serializeValuesWith( // 값을 JSON으로 직렬화
                RedisSerializationContext.SerializationPair
                    .fromSerializer(GenericJackson2JsonRedisSerializer())
            )
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .build()
    }

    companion object {
        const val MAXIMUM_SIZE = 500L
        const val EXPIRE_AFTER_WRITE = 1L

        const val MENU_CACHE = "menu"
        const val MENUS_CACHE = "menus"
    }
}