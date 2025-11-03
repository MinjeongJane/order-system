package order.common.config.cache

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.stereotype.Component

@Component
class CacheInvalidationPublisher(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val topic: ChannelTopic
) {
    fun publish(message: CacheInvalidateMessage) {
        redisTemplate.convertAndSend(topic.topic, message)
    }
}