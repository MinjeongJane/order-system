package order.common.config.cache

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronizationManager

class CacheInvalidationManagerTest {
    @Test
    fun 트랜잭션_비활성일때_전체무효화_즉시_발행() {
        val publisher = mockk<CacheInvalidationPublisher>(relaxed = true)
        val manager = CacheInvalidationManager(publisher)

        manager.invalidateAll("menu-cache")

        verify { publisher.publish(match { it.cacheName == "menu-cache" && it.allEntries }) }
    }

    @Test
    fun 트랜잭션_비활성일때_단건무효화_즉시_발행() {
        val publisher = mockk<CacheInvalidationPublisher>(relaxed = true)
        val manager = CacheInvalidationManager(publisher)

        manager.invalidate("menu-cache", "42")

        verify { publisher.publish(match { it.cacheName == "menu-cache" && it.key == "42" && !it.allEntries }) }
    }

    @Test
    fun 트랜잭션_활성일때_전체무효화_커밋후_발행() {
        val publisher = mockk<CacheInvalidationPublisher>(relaxed = true)
        val manager = CacheInvalidationManager(publisher)

        TransactionSynchronizationManager.initSynchronization()
        TransactionSynchronizationManager.setActualTransactionActive(true)
        try {
            manager.invalidateAll("menu-cache")

            verify(exactly = 0) { publisher.publish(any()) }

            val syncs = TransactionSynchronizationManager.getSynchronizations()
            syncs.forEach { it.afterCommit() }

            verify { publisher.publish(match { it.cacheName == "menu-cache" && it.allEntries }) }
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
            TransactionSynchronizationManager.setActualTransactionActive(false)
        }
    }

    @Test
    fun 여러_무효화_요청이_있을때_모두_커밋후_발행() {
        val publisher = mockk<CacheInvalidationPublisher>(relaxed = true)
        val manager = CacheInvalidationManager(publisher)

        TransactionSynchronizationManager.initSynchronization()
        TransactionSynchronizationManager.setActualTransactionActive(true)
        try {
            manager.invalidate("menu-cache", "1")
            manager.invalidateAll("menu-cache")
            manager.invalidate("menu-cache", "2")

            verify(exactly = 0) { publisher.publish(any()) }

            val syncs = TransactionSynchronizationManager.getSynchronizations()
            syncs.forEach { it.afterCommit() }

            verify { publisher.publish(match { it.cacheName == "menu-cache" && it.key == "1" && !it.allEntries }) }
            verify { publisher.publish(match { it.cacheName == "menu-cache" && it.allEntries }) }
            verify { publisher.publish(match { it.cacheName == "menu-cache" && it.key == "2" && !it.allEntries }) }
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
            TransactionSynchronizationManager.setActualTransactionActive(false)
        }
    }
}