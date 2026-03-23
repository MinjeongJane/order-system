package order.common

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import order.domain.exception.ErrorCode

class GlobalExceptionHandlerTest : DescribeSpec({

    val handler = GlobalExceptionHandler()

    describe("handleException") {
        context("일반 예외가 발생했을 때") {
            it("500 INTERNAL_ERROR 응답을 반환한다") {
                // given
                val exception = RuntimeException("서버 오류")

                // when
                val response = handler.handleException(exception)

                // then
                response.code shouldBe 500
                response.message shouldBe "서버 오류"
            }
        }

        context("메시지 없는 일반 예외가 발생했을 때") {
            it("기본 에러 메시지를 반환한다") {
                // given
                val exception = RuntimeException()

                // when
                val response = handler.handleException(exception)

                // then
                response.code shouldBe 500
                response.message shouldBe ErrorCode.INTERNAL_ERROR.message
            }
        }
    }

    describe("handleIllegalArgumentException") {
        context("잘못된 인수 예외가 발생했을 때") {
            it("400 INVALID_INPUT_VALUE 응답을 반환한다") {
                // given
                val exception = IllegalArgumentException("잘못된 요청입니다.")

                // when
                val response = handler.handleIllegalArgumentException(exception)

                // then
                response.code shouldBe 400
                response.message shouldBe "잘못된 요청입니다."
            }
        }

        context("메시지 없는 잘못된 인수 예외가 발생했을 때") {
            it("기본 에러 메시지를 반환한다") {
                // given
                val exception = IllegalArgumentException()

                // when
                val response = handler.handleIllegalArgumentException(exception)

                // then
                response.code shouldBe 400
                response.message shouldBe ErrorCode.INVALID_INPUT_VALUE.message
            }
        }
    }
})
