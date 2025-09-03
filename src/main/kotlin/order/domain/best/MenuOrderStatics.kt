package order.domain.best

import java.time.LocalDate
import java.time.LocalDateTime

data class MenuOrderStatics(
    val id: Long,
    val date: LocalDate,
    val menuId: Int,
    val count: Int,
    val createdDate: LocalDateTime,
    val updatedDate: LocalDateTime,
)