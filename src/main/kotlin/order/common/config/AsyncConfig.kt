package order.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@EnableAsync
@Configuration
class AsyncConfig {
    @Bean(name = ["menuTaskExecutor"])
    fun menuTaskExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5 // 최소 스레드 수
        executor.maxPoolSize = 10 // 최대 스레드 수
        executor.queueCapacity = 25 // 대기열 크기
        executor.setThreadNamePrefix("MenuExecutor-")
        executor.initialize()
        return executor
    }
}