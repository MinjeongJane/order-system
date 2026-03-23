package order.api

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import order.api.dto.MenuRequest
import order.application.menu.MenuService

class AdminControllerUnitTest : DescribeSpec({

    val menuService = mockk<MenuService>()
    val adminController = AdminController(menuService)

    describe("saveMenu") {
        context("유효한 메뉴 목록이 주어졌을 때") {
            it("메뉴를 저장하고 성공 메시지를 반환한다") {
                // given
                val menuRequests = listOf(
                    MenuRequest(id = null, name = "딸기", price = 3500),
                    MenuRequest(id = null, name = "레몬", price = 2500)
                )
                every { menuService.saveMenu(menuRequests) } just Runs

                // when
                val response = adminController.saveMenu(menuRequests)

                // then
                response.code shouldBe 200
                response.value shouldBe "메뉴 저장이 완료되었습니다."
                verify(exactly = 1) { menuService.saveMenu(menuRequests) }
            }
        }

        context("빈 메뉴 목록이 주어졌을 때") {
            it("빈 목록을 저장하고 성공 메시지를 반환한다") {
                // given
                val emptyRequests = emptyList<MenuRequest>()
                every { menuService.saveMenu(emptyRequests) } just Runs

                // when
                val response = adminController.saveMenu(emptyRequests)

                // then
                response.code shouldBe 200
                response.value shouldBe "메뉴 저장이 완료되었습니다."
            }
        }
    }
})
