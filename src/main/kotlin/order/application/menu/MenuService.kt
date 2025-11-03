package order.application.menu

import order.api.dto.MenuRequest
import order.common.config.cache.CacheInvalidationManager
import order.domain.menu.Menu
import order.domain.menu.MenuRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MenuService(
    private val menuRepository: MenuRepository,
    private val cacheInvalidation: CacheInvalidationManager,
) {
    //    @Async("menuTaskExecutor") @Async와 TaskExecutor를 사용하여 메뉴 조회 작업을 전용 스레드 풀에서 실행하도록 설정
    @Cacheable(cacheNames = ["menu"], key = "'ALL'", sync = true)
    fun findMenuAll(): List<Menu> {
        return menuRepository.findMenuAll()
    }

    @Cacheable(cacheNames = ["menus"], key = "#menuIds", cacheResolver = "cacheResolver", sync = true)
    fun findMenuByIds(menuIds: List<Int>): List<Menu> {
        return menuRepository.findMenuByIds(menuIds)
    }

    @Transactional
    fun saveMenu(menus: List<MenuRequest>) {
        menuRepository.saveMenu(menus)

        cacheInvalidation.invalidateAll("menu")
        cacheInvalidation.invalidateAll("menus")
    }
}