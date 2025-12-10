package order.application.best

import order.domain.best.BestMenu
import order.domain.best.BestRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BestService(
    private val bestRepository: BestRepository,
) {
    @Transactional(readOnly = true)
    fun findBestMenu(): List<BestMenu> {
        return bestRepository.getOrCacheBestMenu()
    }
}