package order.application.menu

import order.domain.menu.Menu
import order.domain.menu.MenuRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class MenuService(
    private val menuRepository: MenuRepository
) {
//    @Async("menuTaskExecutor") @Async와 TaskExecutor를 사용하여 메뉴 조회 작업을 전용 스레드 풀에서 실행하도록 설정
    @Cacheable(cacheNames = ["menu"], sync = true)
    fun findMenuAll(): List<Menu> {
        return menuRepository.findMenuAll()
    }

    @Cacheable(cacheNames = ["menus"], sync = true)
    fun findMenuByIds(menuIds: List<Int>): List<Menu> {
        return menuRepository.findMenuByIds(menuIds)
    }
}