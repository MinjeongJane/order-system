package order.application.admin

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import order.api.dto.MenuRequest
import order.domain.menu.MenuRepository

class AdminServiceTest : DescribeSpec({

    val menuRepository = mockk<MenuRepository>()
    val adminService = AdminService(menuRepository)

    describe("saveMenu") {
        context("유효한 메뉴 요청 목록이 주어졌을 때") {
            it("메뉴 저장소에 저장을 위임한다") {
                // given
                val menuRequests = listOf(
                    MenuRequest(id = null, name = "민트초코", price = 4000),
                    MenuRequest(id = null, name = "피스타치오", price = 5000)
                )
                every { menuRepository.saveMenu(menuRequests) } just Runs

                // when
                adminService.saveMenu(menuRequests)

                // then
                verify(exactly = 1) { menuRepository.saveMenu(menuRequests) }
            }
        }

        context("빈 목록이 주어졌을 때") {
            it("빈 목록으로 저장을 위임한다") {
                // given
                val emptyRequests = emptyList<MenuRequest>()
                every { menuRepository.saveMenu(emptyRequests) } just Runs

                // when
                adminService.saveMenu(emptyRequests)

                // then
                verify(exactly = 1) { menuRepository.saveMenu(emptyRequests) }
            }
        }
    }
})
