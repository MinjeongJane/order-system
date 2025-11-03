package order.common.config.cache

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cache.CacheManager
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.stereotype.Component

@Component
class CacheInvalidationSubscriber(
    private val cacheManagers: List<CacheManager>,
    private val objectMapper: ObjectMapper
) : MessageListener {

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val payload = objectMapper.readValue(message.body, CacheInvalidateMessage::class.java)
        val cache = cacheManagers.firstNotNullOfOrNull { it.getCache(payload.cacheName) } ?: return

        if (payload.allEntries) cache.clear()
        else payload.key?.let { cache.evict(it) }
    }
}