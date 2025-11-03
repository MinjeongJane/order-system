package order.common.config.cache

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class CacheInvalidationManager(
    private val publisher: CacheInvalidationPublisher
) {
    fun invalidateAll(cacheName: String) =
        runAfterCommit {
            publisher.publish(CacheInvalidateMessage(cacheName = cacheName, allEntries = true))
        }

    fun invalidate(cacheName: String, key: String) =
        runAfterCommit {
            publisher.publish(CacheInvalidateMessage(cacheName = cacheName, key = key))
        }

    private fun runAfterCommit(action: () -> Unit) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() = action()
            })
        } else {
            action()
        }
    }
}