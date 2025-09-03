package order.domain.exception


enum class ErrorCode(val code: Int, val message: String) {
    INVALID_INPUT_VALUE(400, "잘못된 입력 값입니다."),
    NOT_FOUND(404, "리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR(500, "서버 내부 오류입니다.")
}
