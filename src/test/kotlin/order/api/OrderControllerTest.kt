package order.api

import java.nio.charset.StandardCharsets
import order.application.best.BestService
import order.application.menu.MenuService
import order.application.order.OrderService
import order.application.user.UserCreditService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(OrderController::class)
class OrderControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var bestService: BestService

    @MockBean
    lateinit var menuService: MenuService

    @MockBean
    lateinit var creditService: UserCreditService

    @MockBean
    lateinit var orderService: OrderService

    @Test
    fun `인기메뉴 없을 때 집계 없음 메시지 반환`() {
        given(bestService.findBestMenu()).willReturn(emptyList())

        val result = mockMvc.get("/api/order/best-menu")
            .andExpect { status { isOk() } }
            .andReturn()

        val response = result.response.getContentAsString(StandardCharsets.UTF_8)
        assert(response.contains("집계 된 메뉴가 없습니다."))
    }
}