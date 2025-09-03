package order.api.dto

import order.domain.exception.ErrorCode

data class Response<T>(
    val code: Int,
    val message: String,
    val value: T? = null
) {
    companion object {
        fun <T> ok(value: T): Response<T> = Response(0, "ok", value)

        fun error(errorCode: ErrorCode): Response<Nothing> = Response(errorCode.code, errorCode.message)

        fun error(errorCode: ErrorCode, message: String?): Response<Nothing> =
            Response(errorCode.code, message ?: errorCode.message)
    }
}