package order.common.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedissonConfig(
    @Value("\${spring.data.redis.host}") private val redisHost: String,
    @Value("\${spring.data.redis.port}") private val redisPort: Int
) {
    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config()
        config.useSingleServer().apply {
            address = "redis://$redisHost:$redisPort"
            connectionPoolSize = 64
            connectionMinimumIdleSize = 24
            idleConnectionTimeout = 10000
            connectTimeout = 10000
            timeout = 3000
            retryAttempts = 3
            retryInterval = 1500
            subscriptionConnectionPoolSize = 50
        }
        config.codec = StringCodec()
        config.lockWatchdogTimeout = 30000L // 분산락 워치독 타임아웃(기본 30초, 단위 ms)
        return Redisson.create(config)
    }

    @Bean
    @ConfigurationProperties(prefix = "app.distributed-lock")
    fun distributedLockProperties(): DistributedLockProperties {
        return DistributedLockProperties()
    }
}

/**
 * 분산 락 설정 Properties
 */
@ConfigurationProperties(prefix = "app.distributed-lock")
data class DistributedLockProperties(
    /**
     * 기본 대기 시간 (초)
     */
    var defaultWaitTime: Long = 5L,

    /**
     * 기본 임대 시간 (초)
     */
    var defaultLeaseTime: Long = 30L,

    /**
     * 락 워치독 타임아웃 (밀리초)
     */
    var watchdogTimeout: Long = 30000L,

    /**
     * 락 키 프리픽스
     */
    var keyPrefix: String = "lock"
)