package order.common.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.util.concurrent.TimeUnit

class DistributedLockAspectTest : DescribeSpec({

    val redissonClient = mockk<RedissonClient>()
    val aspect = DistributedLockAspect(redissonClient)

    afterTest { clearAllMocks() }

    describe("around") {
        context("락 획득 성공 시") {
            it("joinPoint.proceed()를 실행하고 결과를 반환한다") {
                // given
                val lock = mockk<RLock>(relaxed = true)
                every { redissonClient.getLock(any()) } returns lock
                every { lock.tryLock(any(), any(), any()) } returns true
                every { lock.isHeldByCurrentThread } returns true

                val method = SimpleTestService::class.java.getMethod("process", String::class.java)
                val signature = mockk<MethodSignature>()
                every { signature.method } returns method
                every { signature.parameterNames } returns arrayOf("userId")

                val joinPoint = mockk<ProceedingJoinPoint>()
                every { joinPoint.signature } returns signature
                every { joinPoint.args } returns arrayOf("user123")
                every { joinPoint.proceed() } returns "success"

                // when
                val result = aspect.around(joinPoint)

                // then
                result shouldBe "success"
                verify(exactly = 1) { joinPoint.proceed() }
                verify(exactly = 1) { lock.unlock() }
            }
        }

        context("락 획득 실패 시") {
            it("failureMessage로 IllegalStateException을 발생시킨다") {
                // given
                val lock = mockk<RLock>(relaxed = true)
                every { redissonClient.getLock(any()) } returns lock
                every { lock.tryLock(any(), any(), any()) } returns false
                every { lock.isHeldByCurrentThread } returns false

                val method = SimpleTestService::class.java.getMethod("process", String::class.java)
                val signature = mockk<MethodSignature>()
                every { signature.method } returns method
                every { signature.parameterNames } returns arrayOf("userId")

                val joinPoint = mockk<ProceedingJoinPoint>()
                every { joinPoint.signature } returns signature
                every { joinPoint.args } returns arrayOf("user123")

                // when & then
                val exception = shouldThrow<IllegalStateException> {
                    aspect.around(joinPoint)
                }
                exception.message shouldBe "락 획득 실패 테스트"
            }
        }

        context("keyExpression이 없을 때") {
            it("기본 키만 사용하여 락을 시도한다") {
                // given
                val lock = mockk<RLock>(relaxed = true)
                every { redissonClient.getLock("simple-lock") } returns lock
                every { lock.tryLock(any(), any(), any()) } returns true
                every { lock.isHeldByCurrentThread } returns true

                val method = SimpleTestService::class.java.getMethod("simpleProcess")
                val signature = mockk<MethodSignature>()
                every { signature.method } returns method
                every { signature.parameterNames } returns emptyArray()

                val joinPoint = mockk<ProceedingJoinPoint>()
                every { joinPoint.signature } returns signature
                every { joinPoint.args } returns emptyArray()
                every { joinPoint.proceed() } returns "ok"

                // when
                val result = aspect.around(joinPoint)

                // then
                result shouldBe "ok"
                verify { redissonClient.getLock("simple-lock") }
            }
        }
    }
})

// 테스트용 서비스 클래스
class SimpleTestService {
    @DistributedLock(
        key = "test-lock",
        keyExpression = "#userId",
        waitTime = 3L,
        leaseTime = 30L,
        timeUnit = TimeUnit.SECONDS,
        failureMessage = "락 획득 실패 테스트"
    )
    fun process(userId: String): String = "processed: $userId"

    @DistributedLock(
        key = "simple-lock",
        waitTime = 3L,
        leaseTime = 30L
    )
    fun simpleProcess(): String = "simple"
}
