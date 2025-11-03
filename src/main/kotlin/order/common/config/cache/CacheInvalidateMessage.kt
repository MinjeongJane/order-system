package order.common.config.cache

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CacheInvalidateMessage(
    val cacheName: String,
    val key: String? = null,
    val allEntries: Boolean = false
)