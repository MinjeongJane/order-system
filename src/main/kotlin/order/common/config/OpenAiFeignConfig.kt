package order.common.config

import feign.RequestInterceptor
import feign.RequestTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenAiFeignConfig {

    @Value("\${openai.api-key}")
    private lateinit var apiKey: String

    @Bean
    fun authInterceptor(): RequestInterceptor = RequestInterceptor { tmpl: RequestTemplate ->
        tmpl.header("Authorization", "Bearer $apiKey")   // Bearer 인증
        tmpl.header("Content-Type", "application/json")
    }
}