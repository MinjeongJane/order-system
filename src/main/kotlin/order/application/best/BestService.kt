package order.application.best

import order.domain.best.BestMenu
import order.domain.best.BestRepository
import org.springframework.stereotype.Service

@Service
class BestService(
    private val bestRepository: BestRepository,
) {
    fun findBestMenu(): List<BestMenu> {
        return bestRepository.getOrCacheBestMenu()
    }
}