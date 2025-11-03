package order.common.config.cache

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class CacheInvalidationConfig {
    @Bean
    fun cacheInvalidationTopic(): ChannelTopic =
        ChannelTopic("cache:invalidate")

    @Bean
    fun cacheInvalidationContainer(
        factory: RedisConnectionFactory,
        subscriber: CacheInvalidationSubscriber,
        topic: ChannelTopic
    ): RedisMessageListenerContainer =
        RedisMessageListenerContainer().apply {
            setConnectionFactory(factory)
            addMessageListener(subscriber, topic)
        }

    @Bean
    fun cacheInvalidationRedisTemplate(
        factory: RedisConnectionFactory
    ): RedisTemplate<String, Any> =
        RedisTemplate<String, Any>().apply {
            setConnectionFactory(factory)
            keySerializer = StringRedisSerializer()
            valueSerializer = GenericJackson2JsonRedisSerializer()
            afterPropertiesSet()
        }
}