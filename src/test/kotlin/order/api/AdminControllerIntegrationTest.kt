package order.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import order.common.config.TestJpaConfig
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJpaConfig::class)
@ActiveProfiles("test")
class AdminControllerIntegrationTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
    val jdbcTemplate: JdbcTemplate,
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

