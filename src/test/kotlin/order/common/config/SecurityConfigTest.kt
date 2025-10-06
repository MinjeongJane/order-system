package order.common.config

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var securityFilterChain: SecurityFilterChain

    @Test
    fun `SecurityFilterChain 빈 정상 생성`() {
        assertNotNull(securityFilterChain)
    }

    @Test
    fun `모든 엔드포인트는 인증없이 접근 가능`() {
        mockMvc.get("/public/hello")
            .andExpect { status { isOk() } }
    }
}