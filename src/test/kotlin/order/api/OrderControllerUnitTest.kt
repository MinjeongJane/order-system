package order.api

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import order.api.dto.BestMenuResponse
import order.api.dto.CreditChargeRequest
import order.api.dto.OrderDetailsRequest
import order.api.dto.OrderRequest
import order.application.best.BestService
import order.application.menu.MenuService
import order.application.order.OrderService
import order.application.user.UserCreditService
import order.domain.best.BestMenu
import order.domain.menu.Menu
import order.domain.order.OrderHistory
import order.domain.order.OrderHistoryResult
import java.time.LocalDateTime

class OrderControllerUnitTest : DescribeSpec({

    val menuService = mockk<MenuService>()
    val creditService = mockk<UserCreditService>()
    val orderService = mockk<OrderService>()
    val bestService = mockk<BestService>()
    val orderController = OrderController(menuService, creditService, orderService, bestService)

    afterTest { clearAllMocks() }

    describe("findMenu") {
        context("메뉴가 존재할 때") {
            it("메뉴 목록을 Response로 감싸서 반환한다") {
                // given
                val menus = listOf(
                    Menu(1L, "바닐라", 3000),
                    Menu(2L, "초코", 4000)
                )
                every { menuService.findMenuAll() } returns menus

                // when
                val response = orderController.findMenu()

                // then
                response.code shouldBe 200
                response.value!!.size shouldBe 2
                response.value!![0].name shouldBe "바닐라"
                response.value!![1].price shouldBe 4000
            }
        }

        context("메뉴가 없을 때") {
            it("빈 목록을 반환한다") {
                every { menuService.findMenuAll() } returns emptyList()

                val response = orderController.findMenu()

                response.code shouldBe 200
                response.value!!.isEmpty() shouldBe true
            }
        }
    }

    describe("chargeCredit") {
        context("유효한 충전 요청이 주어졌을 때") {
            it("충전 성공 메시지를 반환한다") {
                // given
                val request = CreditChargeRequest(userId = 1L, credits = 5000)
                every { creditService.charge(1L, 5000) } returns Unit

                // when
                val response = orderController.chargeCredit(request)

                // then
                response.code shouldBe 200
                response.value shouldBe "크레딧 충전이 완료되었습니다."
                verify(exactly = 1) { creditService.charge(1L, 5000) }
            }
        }
    }

    describe("order") {
        context("유효한 주문 요청이 주어졌을 때") {
            it("주문 완료 메시지를 반환한다") {
                // given
                val orderDetails = listOf(OrderDetailsRequest(menuId = 1, count = 2, menuPrice = 3000))
                val request = OrderRequest(userId = 1L, orderDetails = orderDetails, price = 6000)
                val now = LocalDateTime.now()
                val orderHistory = OrderHistory(
                    id = 100L,
                    userId = 1L,
                    price = 6000,
                    createdBy = "system",
                    createdAt = now,
                    modifiedBy = "system",
                    modifiedAt = now
                )
                every { orderService.order(request) } returns orderHistory

                // when
                val response = orderController.order(request)

                // then
                response.code shouldBe 200
                response.value!!.contains("1") shouldBe true
                response.value!!.contains("6000") shouldBe true
            }
        }
    }

    describe("findOrderHistory") {
        context("주문 내역이 있는 사용자 ID가 주어졌을 때") {
            it("OrderHistoryResponse 목록을 반환한다") {
                val now = LocalDateTime.now()
                val orderHistory = OrderHistory(id = 1L, userId = 1L, price = 6000, createdBy = "test", createdAt = now, modifiedBy = "test", modifiedAt = now)
                val results = listOf(OrderHistoryResult(history = orderHistory, details = emptyList()))
                every { orderService.findOrdersByUserId(1L) } returns results

                val response = orderController.findOrderHistory(1L)

                response.code shouldBe 200
                response.value!!.size shouldBe 1
                response.value!![0].orderId shouldBe 1L
                response.value!![0].price shouldBe 6000
            }
        }

        context("주문 내역이 없을 때") {
            it("빈 목록을 반환한다") {
                every { orderService.findOrdersByUserId(any()) } returns emptyList()

                val response = orderController.findOrderHistory(999L)

                response.code shouldBe 200
                response.value!!.isEmpty() shouldBe true
            }
        }
    }

    describe("findBestMenu") {
        context("인기 메뉴가 존재할 때") {
            it("메뉴 이름과 주문 수를 포함한 응답을 반환한다") {
                // given
                val bestMenus = listOf(
                    BestMenu(menuId = 1, orderCount = 100),
                    BestMenu(menuId = 2, orderCount = 50)
                )
                val menus = listOf(
                    Menu(1L, "바닐라", 3000),
                    Menu(2L, "초코", 4000)
                )
                every { bestService.findBestMenu() } returns bestMenus
                every { menuService.findMenuByIds(listOf(1, 2)) } returns menus

                // when
                val response = orderController.findBestMenu()

                // then
                response.code shouldBe 200
                @Suppress("UNCHECKED_CAST")
                val result = response.value as List<BestMenuResponse>
                result.size shouldBe 2
                result[0].name shouldBe "바닐라"
                result[0].orderCount shouldBe 100
            }
        }

        context("인기 메뉴가 없을 때") {
            it("집계 없음 메시지를 반환한다") {
                every { bestService.findBestMenu() } returns emptyList()

                val response = orderController.findBestMenu()

                response.code shouldBe 200
                response.value shouldBe "집계 된 메뉴가 없습니다."
            }
        }
    }
})
