package order.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import order.application.menu.MenuService
import order.common.config.TestJpaConfig
import order.domain.menu.Menu
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.nio.charset.StandardCharsets
import java.sql.Timestamp

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJpaConfig::class)
@ActiveProfiles("test")
class AdminControllerIntegrationTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
    val jdbcTemplate: JdbcTemplate,
    val menuService: MenuService,
) {
    private val testMenuId = 77777777

    @AfterEach
    fun tearDown() {
        jdbcTemplate.update("DELETE FROM MENU WHERE id = ?", testMenuId)
    }

    @Test
    fun `메뉴 저장 후 성공 응답 반환`() {
        val payload = listOf(
            mapOf(
                "id" to testMenuId,
                "name" to "테스트메뉴",
                "price" to 1234
            )
        )

        val result = mockMvc.post("/api/admin/menu") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect { status { isOk() } }
            .andReturn()

        val response = result.response.getContentAsString(StandardCharsets.UTF_8)
        val json: JsonNode = objectMapper.readTree(response)

        assert(json["code"].asInt() == 200)
        assert(json["message"].asText() == "ok")
        assert(json["value"].asText() == "메뉴 저장이 완료되었습니다.")
    }

    @Test
    fun `캐시 무효화 이후 DB에서 최신 메뉴를 읽어오는지 확인`() {
        val now = Timestamp(System.currentTimeMillis())
        // 초기 데이터 삽입 (캐시에 초기값 적재 목적)
        jdbcTemplate.update("DELETE FROM MENU WHERE id = ?", testMenuId)
        jdbcTemplate.update(
            "INSERT INTO MENU (id, name, price, created_by, created_at, modified_by, modified_at, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            testMenuId,
            "초기메뉴",
            1000,
            "admin",
            now,
            "admin",
            now,
            0
        )

        // 캐시 채움
        val before = menuService.findMenuAll().firstOrNull { it.id == testMenuId.toLong() }
        assert(before != null && before.price == 1000)

        // 메뉴 업데이트(내부에서 트랜잭션 커밋 후 캐시 무효화가 일어남)
        val payload = listOf(
            mapOf(
                "id" to testMenuId,
                "name" to "초기메뉴",
                "price" to 2000
            )
        )

        mockMvc.post("/api/admin/menu") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect { status { isOk() } }

        // 캐시 무효화가 반영될 때까지 폴링하여 최신값 확인
        val expectedPrice = 2000
        var found = false
        var actualPrice = -1

        repeat(10) { // 최대 1초 (10 * 100ms)
            val menus: List<Menu> = menuService.findMenuAll()
            val menu = menus.firstOrNull { it.id == testMenuId.toLong() }
            if (menu != null) {
                actualPrice = menu.price
                if (actualPrice == expectedPrice) {
                    found = true
                    return@repeat
                }
            }
            Thread.sleep(100)
        }

        assert(found) { "캐시 무효화가 반영되지 않았습니다. 실제 가격: $actualPrice" }
    }

    @Test
    fun `가격이 음수이면 BadRequest 반환`() {
        val payload = listOf(
            mapOf(
                "id" to testMenuId,
                "name" to "음수가격메뉴",
                "price" to -100
            )
        )

        val result = mockMvc.post("/api/admin/menu") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect { status { isOk() } }
            .andReturn()

        val response = result.response.getContentAsString(StandardCharsets.UTF_8)
        val json: JsonNode = objectMapper.readTree(response)

        assert(json["code"].asInt() != 200)
        assert(json["message"].asText().contains("BAD_REQUEST"))
    }
}

