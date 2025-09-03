package order.common.config

import java.util.concurrent.TimeUnit

/**
 * 분산 락 어노테이션
 *
 * 사용 예시:
 * @DistributedLock(key = "order:processing", keyExpression = "#request.userId")
 * fun processOrder(request: OrderRequest): OrderHistory { ... }
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
    /**
     * 락 키 이름 (기본 프리픽스)
     */
    val key: String,

    /**
     * 동적 키 생성을 위한 SpEL 표현식
     * 예: "#request.userId", "#userId + ':' + #menuId"
     */
    val keyExpression: String = "",

    /**
     * 락 획득 대기 시간 (초)
     */
    val waitTime: Long = DEFAULT_WAIT_TIME,

    /**
     * 락 자동 해제 시간 (초)
     */
    val leaseTime: Long = DEFAULT_LEASE_TIME,

    /**
     * 시간 단위
     */
    val timeUnit: TimeUnit = TimeUnit.SECONDS,

    /**
     * 락 획득 실패 시 예외 메시지
     */
    val failureMessage: String = DEFAULT_FAILURE_MESSAGE
) {
    companion object {
        const val DEFAULT_WAIT_TIME = 5L
        const val DEFAULT_LEASE_TIME = 30L
        const val DEFAULT_FAILURE_MESSAGE = "처리 중입니다. 잠시 후 다시 시도해주세요."
    }
}