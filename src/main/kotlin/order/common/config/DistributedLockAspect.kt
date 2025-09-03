package order.common.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 트랜잭션보다 먼저 실행되도록 설정
class DistributedLockAspect(
    private val redissonClient: RedissonClient
) {
    private val logger = KotlinLogging.logger {}
    private val parser: ExpressionParser = SpelExpressionParser()

    @Around("@annotation(distributedLock)")
    fun around(joinPoint: ProceedingJoinPoint, distributedLock: DistributedLock): Any? {
        val lockKey = generateLockKey(joinPoint, distributedLock)
        val lock = redissonClient.getLock(lockKey)

        logger.debug { "분산 락 시도 - 키: $lockKey" }

        return try {
            val acquired = lock.tryLock(
                distributedLock.waitTime,
                distributedLock.leaseTime,
                distributedLock.timeUnit
            )

            if (!acquired) {
                logger.warn { "분산 락 획득 실패 - 키: $lockKey" }
                throw IllegalStateException(distributedLock.failureMessage)
            }

            logger.debug { "분산 락 획득 성공 - 키: $lockKey" }
            joinPoint.proceed()

        } catch (e: InterruptedException) {
            logger.error(e) { "분산 락 대기 중 인터럽트 발생 - 키: $lockKey" }
            Thread.currentThread().interrupt()
            throw IllegalStateException("락 처리가 중단되었습니다.", e)
        } finally {
            releaseLockSafely(lock, lockKey)
        }
    }

    /**
     * 락 키 생성
     * - 기본 키 + SpEL 표현식으로 동적 키 생성
     */
    private fun generateLockKey(joinPoint: ProceedingJoinPoint, distributedLock: DistributedLock): String {
        val baseKey = distributedLock.key

        if (distributedLock.keyExpression.isEmpty()) {
            return baseKey
        }

        return try {
            val context = createEvaluationContext(joinPoint)
            val expression = parser.parseExpression(distributedLock.keyExpression)
            val dynamicKey = expression.getValue(context, String::class.java) ?: ""

            "$baseKey:$dynamicKey"
        } catch (e: Exception) {
            logger.error(e) { "락 키 생성 실패, 기본 키 사용: $baseKey" }
            baseKey
        }
    }

    /**
     * SpEL 평가 컨텍스트 생성
     */
    private fun createEvaluationContext(joinPoint: ProceedingJoinPoint): StandardEvaluationContext {
        val context = StandardEvaluationContext()

        // 메서드 파라미터를 SpEL 변수로 등록
        val signature = joinPoint.signature as MethodSignature
        val parameterNames = signature.parameterNames
        val args = joinPoint.args

        for (i in parameterNames.indices) {
            context.setVariable(parameterNames[i], args[i])
        }

        return context
    }

    /**
     * 안전한 락 해제
     */
    private fun releaseLockSafely(lock: RLock, lockKey: String) {
        try {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                logger.debug { "분산 락 해제 완료 - 키: $lockKey" }
            }
        } catch (e: Exception) {
            logger.error(e) { "분산 락 해제 중 오류 발생 - 키: $lockKey" }
        }
    }
}