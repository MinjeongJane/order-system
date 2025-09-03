import order.OrderSystemApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [OrderSystemApplication::class])
class OrderSystemApplicationTest {

    @Test
    fun contextLoads() {
        // 애플리케이션 컨텍스트가 정상적으로 로드되는지만 확인
    }
}