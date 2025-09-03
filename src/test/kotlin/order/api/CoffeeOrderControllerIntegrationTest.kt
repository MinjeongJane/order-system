//package order.api
//
//import order.api.dto.OrderDetailsRequest
//import order.api.dto.OrderRequest
//import order.api.dto.PointChargeRequest
//import order.common.TestJpaConfig
//import com.fasterxml.jackson.databind.JsonNode
//import com.fasterxml.jackson.databind.ObjectMapper
//import java.nio.charset.StandardCharsets
//import org.junit.jupiter.api.AfterEach
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.context.annotation.Import
//import org.springframework.http.MediaType
//import org.springframework.jdbc.core.JdbcTemplate
//import org.springframework.test.context.ActiveProfiles
//import org.springframework.test.web.servlet.MockMvc
//import org.springframework.test.web.servlet.get
//import org.springframework.test.web.servlet.post
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@Import(TestJpaConfig::class)
//@ActiveProfiles("test")
//class OrderControllerIntegrationTest @Autowired constructor(
//    val mockMvc: MockMvc,
//    val objectMapper: ObjectMapper,
//    val jdbcTemplate: JdbcTemplate,
//) {
//    private val testUserId = 99999L
//    private val testMenuId = 99999
//
//    @BeforeEach
//    fun setUp() {
//        // 테스트용 사용자 추가
//        jdbcTemplate.update(
//            "INSERT IGNORE INTO USER_POINT (id, points, created_date, updated_date, deleted) VALUES (?, '1000000', '2025-07-17 09:00:00', '2025-07-17 09:00:00', '0')",
//            testUserId
//        )
//        jdbcTemplate.update(
//            "INSERT IGNORE INTO MENU (id, name, price, created_date, updated_date, deleted) VALUES (?, '프라푸치노', 6000, '2025-07-17 09:00:00', '2025-07-17 09:00:00', '0')",
//            testMenuId
//        )
//    }
//
//    @AfterEach
//    fun tearDown() {
//        // 테스트용 메뉴 삭제
//        jdbcTemplate.update("DELETE FROM MENU WHERE id = ?", testMenuId)
//
//        // 주문 내역 삭제
//        val orderIds = jdbcTemplate.queryForList(
//            "SELECT id FROM ORDER_HISTORY WHERE user_id = ?", Long::class.java, testUserId
//        )
//        if (orderIds.isNotEmpty()) {
//            // 해당 주문 id와 menu_id로 상세 내역 삭제
//            jdbcTemplate.update(
//                "DELETE FROM ORDER_DETAILS WHERE order_id IN (${orderIds.joinToString(",")}) AND menu_id = ?",
//                testMenuId
//            )
//        }
//        jdbcTemplate.update("DELETE FROM ORDER_HISTORY WHERE user_id = ?", testUserId)
//
//        // 포인트 충전 내역 삭제
//        jdbcTemplate.update("DELETE FROM USER_POINT WHERE id = ?", testUserId)
//
//        // 주문 통계 내역 삭제
//        jdbcTemplate.update("DELETE FROM MENU_ORDER_STATISTICS WHERE menu_id = ?", testMenuId)
//    }
//
//    @Test
//    fun `메뉴 조회시 200과 메뉴 리스트 반환`() {
//        val result = mockMvc.get("/api/order/menu")
//            .andExpect { status { isOk() } }
//            .andReturn()
//
//        val response = result.response.contentAsString
//        assert(response.contains("id"))
//        assert(response.contains("name"))
//        assert(response.contains("price"))
//    }
//
//    @Test
//    fun `포인트 충전시 성공 메시지 반환`() {
//        val request = PointChargeRequest(userId = testUserId, points = 100)
//        val result = mockMvc.post("/api/order/charge") {
//            contentType = MediaType.APPLICATION_JSON
//            content = objectMapper.writeValueAsString(request)
//        }.andExpect { status { isOk() } }
//            .andReturn()
//
//        val response = result.response.getContentAsString(StandardCharsets.UTF_8)
//        val json: JsonNode = objectMapper.readTree(response)
//
//        assert(json["code"].asInt() == 0)
//        assert(json["message"].asText() == "ok")
//        assert(json["value"].asText() == "포인트 충전이 완료되었습니다.")
//    }
//
//    @Test
//    fun `주문 성공시 주문 완료 메시지 반환`() {
//        val orderDetails = listOf(OrderDetailsRequest(menuId = testMenuId, count = 1, menuPrice = 6000))
//        val request = OrderRequest(userId = testUserId, orderDetails = orderDetails, price = 6000)
//        val result = mockMvc.post("/api/order") {
//            contentType = MediaType.APPLICATION_JSON
//            content = objectMapper.writeValueAsString(request)
//        }.andExpect { status { isOk() } }
//            .andReturn()
//
//        val response = result.response.getContentAsString(StandardCharsets.UTF_8)
//        val json: JsonNode = objectMapper.readTree(response)
//
//        assert(json["code"].asInt() == 0)
//        assert(json["message"].asText() == "ok")
//        assert(json["value"].asText().contains("주문이 완료되었습니다."))
//        assert(json["value"].asText().contains("사용자 ID"))
//        assert(json["value"].asText().contains("총 결제 가격"))
//    }
//}