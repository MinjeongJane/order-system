package order.infrastructure.feign.openai

import order.common.config.OpenAiFeignConfig
import order.infrastructure.feign.openai.dto.ChatCompletionRequest
import order.infrastructure.feign.openai.dto.ChatCompletionResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(
    name = "openaiClient",
    url = "\${openai.base-url}",
    configuration = [OpenAiFeignConfig::class]
)
interface OpenAiFeignClient {
    @PostMapping("/chat/completions")
    fun chatCompletions(@RequestBody req: ChatCompletionRequest): ChatCompletionResponse
}