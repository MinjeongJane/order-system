package order.application.menu

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import order.api.dto.MenuRequest
import order.common.config.cache.CacheInvalidationManager
import order.domain.menu.Menu
import order.domain.menu.MenuRepository

class MenuServiceTest : DescribeSpec({

    val menuRepository = mockk<MenuRepository>()
    val cacheInvalidation = mockk<CacheInvalidationManager>()
    val menuService = MenuService(menuRepository, cacheInvalidation)

    afterTest { clearAllMocks() }

    describe("findMenuAll") {
        context("메뉴가 존재할 때") {
            it("전체 메뉴 목록을 반환한다") {
                // given
                val menus = listOf(
                    Menu(1L, "바닐라", 3000),
                    Menu(2L, "초코", 4000)
                )
                every { menuRepository.findMenuAll() } returns menus

                // when
                val result = menuService.findMenuAll()

                // then
                result.size shouldBe 2
                result[0].name shouldBe "바닐라"
                verify(exactly = 1) { menuRepository.findMenuAll() }
            }
        }

        context("메뉴가 없을 때") {
            it("빈 목록을 반환한다") {
                every { menuRepository.findMenuAll() } returns emptyList()

                val result = menuService.findMenuAll()

                result.isEmpty() shouldBe true
            }
        }
    }

    describe("findMenuByIds") {
        context("유효한 메뉴 ID 목록이 주어졌을 때") {
            it("해당 메뉴 목록을 반환한다") {
                // given
                val menuIds = listOf(1, 2)
                val menus = listOf(
                    Menu(1L, "바닐라", 3000),
                    Menu(2L, "초코", 4000)
                )
                every { menuRepository.findMenuByIds(menuIds) } returns menus

                // when
                val result = menuService.findMenuByIds(menuIds)

                // then
                result.size shouldBe 2
                verify(exactly = 1) { menuRepository.findMenuByIds(menuIds) }
            }
        }

        context("존재하지 않는 ID가 포함된 경우") {
            it("빈 목록을 반환한다") {
                val menuIds = listOf(999)
                every { menuRepository.findMenuByIds(menuIds) } returns emptyList()

                val result = menuService.findMenuByIds(menuIds)

                result.isEmpty() shouldBe true
            }
        }
    }

    describe("saveMenu") {
        context("유효한 메뉴 요청 목록이 주어졌을 때") {
            it("메뉴를 저장하고 캐시를 무효화한다") {
                // given
                val menuRequests = listOf(
                    MenuRequest(id = null, name = "딸기", price = 3500)
                )
                every { menuRepository.saveMenu(menuRequests) } just Runs
                every { cacheInvalidation.invalidateAll("menu") } just Runs
                every { cacheInvalidation.invalidateAll("menus") } just Runs

                // when
                menuService.saveMenu(menuRequests)

                // then
                verify(exactly = 1) { menuRepository.saveMenu(menuRequests) }
                verify(exactly = 1) { cacheInvalidation.invalidateAll("menu") }
                verify(exactly = 1) { cacheInvalidation.invalidateAll("menus") }
            }
        }
    }
})
