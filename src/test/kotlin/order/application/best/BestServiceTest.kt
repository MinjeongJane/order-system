package order.application.best

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import order.domain.best.BestMenu
import order.domain.best.BestRepository

class BestServiceTest : DescribeSpec({

    val bestRepository = mockk<BestRepository>()
    val bestService = BestService(bestRepository)

    afterTest { clearAllMocks() }

    describe("findBestMenu") {
        context("인기 메뉴가 존재할 때") {
            it("인기 메뉴 목록을 반환한다") {
                // given
                val bestMenus = listOf(
                    BestMenu(menuId = 1, orderCount = 100),
                    BestMenu(menuId = 2, orderCount = 50)
                )
                every { bestRepository.getOrCacheBestMenu() } returns bestMenus

                // when
                val result = bestService.findBestMenu()

                // then
                result.size shouldBe 2
                result[0].menuId shouldBe 1
                result[0].orderCount shouldBe 100
                verify(exactly = 1) { bestRepository.getOrCacheBestMenu() }
            }
        }

        context("인기 메뉴가 없을 때") {
            it("빈 목록을 반환한다") {
                // given
                every { bestRepository.getOrCacheBestMenu() } returns emptyList()

                // when
                val result = bestService.findBestMenu()

                // then
                result.isEmpty() shouldBe true
                verify(exactly = 1) { bestRepository.getOrCacheBestMenu() }
            }
        }
    }
})
