package order.common.config

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class Resilience4jConfig {
    @Bean
    fun rateLimiterRegistry(): RateLimiterRegistry {
        val config = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ZERO)
            .build()
        return RateLimiterRegistry.of(mapOf("dbFallbackLimiter" to config))
    }

    @Bean
    fun dbFallbackLimiter(rateLimiterRegistry: RateLimiterRegistry): RateLimiter =
        rateLimiterRegistry.rateLimiter("dbFallbackLimiter")
}