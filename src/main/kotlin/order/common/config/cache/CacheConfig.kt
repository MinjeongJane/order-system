package order.common.config.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.interceptor.CacheOperationInvocationContext
import org.springframework.cache.interceptor.CacheResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig(
    private val cacheLoaderManager: CacheLoaderManager
) {
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
        cacheManager.cacheNames = listOf(MENU_CACHE)
        cacheManager.setCaffeine(caffeine)
        return cacheManager
    }

    @Bean
    fun menusCacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager()

        cacheManager.setCacheLoader { key ->
            when (key) {
                is List<*> -> cacheLoaderManager.loadMenusCache(key.filterIsInstance<Int>())
                else -> throw IllegalArgumentException("Expected List<Int> key for menus cache but got: ${key?.javaClass}")
            }
        }

        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(Duration.ofMinutes(MENUS_EXPIRE_AFTER_WRITE))
                .refreshAfterWrite(Duration.ofMinutes(MENUS_REFRESH_AFTER_WRITE))
        )

        cacheManager.setCacheNames(listOf(MENUS_CACHE))

        return cacheManager
    }

    @Bean
    fun cacheResolver(
        @Qualifier("cacheManager") defaultCacheManager: CacheManager,
        @Qualifier("menusCacheManager") menusCacheManager: CacheManager
    ): CacheResolver {
        return CacheResolver { context: CacheOperationInvocationContext<*> ->
            context.operation.cacheNames.map { name ->
                when (name) {
                    MENUS_CACHE -> menusCacheManager.getCache(name)
                        ?: throw IllegalStateException("No cache '$name' in menusCacheManager")

                    else -> defaultCacheManager.getCache(name)
                        ?: throw IllegalStateException("No cache '$name' in default cacheManager")
                }
            }
        }
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
        const val MENUS_EXPIRE_AFTER_WRITE = 10L
        const val MENUS_REFRESH_AFTER_WRITE = 5L

        const val MENU_CACHE = "menu"
        const val MENUS_CACHE = "menus"
    }
}
