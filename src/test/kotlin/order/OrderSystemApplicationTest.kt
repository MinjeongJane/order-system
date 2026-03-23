import order.OrderSystemApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [OrderSystemApplication::class])
@ActiveProfiles("test")
class OrderSystemApplicationTest {

    @Test
    fun contextLoads() {
        // 애플리케이션 컨텍스트가 정상적으로 로드되는지만 확인
    }
}