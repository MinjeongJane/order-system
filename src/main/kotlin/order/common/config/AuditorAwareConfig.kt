package order.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

@Configuration
class AuditorAwareConfig {
    @Bean
    fun auditorProvider(): AuditorAware<String> = AuditorAware {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated || authentication.principal == "anonymous") {
            Optional.of("system") // 인증 정보 없을 때 기본값
        } else {
            Optional.ofNullable(authentication.name) // 사용자명 반환
        }
    }
}