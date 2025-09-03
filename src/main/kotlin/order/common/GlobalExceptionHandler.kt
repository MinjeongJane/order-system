package order.common

import io.github.oshai.kotlinlogging.KotlinLogging
import order.api.dto.Response
import order.domain.exception.ErrorCode
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): Response<Nothing> {
        logger.error(ex) { ex.message ?: ErrorCode.INTERNAL_ERROR.message }
        return Response.error(ErrorCode.INTERNAL_ERROR, ex.message)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): Response<Nothing> {
        logger.warn(ex) { ex.message ?: ErrorCode.INVALID_INPUT_VALUE.message }
        return Response.error(ErrorCode.INVALID_INPUT_VALUE, ex.message)
    }
}