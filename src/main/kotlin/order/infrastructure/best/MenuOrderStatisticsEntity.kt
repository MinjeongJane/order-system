package order.infrastructure.best

import order.domain.best.MenuOrderStatics
import order.infrastructure.common.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import org.hibernate.annotations.SQLDelete

@Entity
@Table(name = "menu_order_statistics", schema = "order_system")
@SQLDelete(sql = "UPDATE menu_order_statistics SET deleted = true WHERE id = ?")
class MenuOrderStatisticsEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var date: LocalDate? = LocalDate.now(),

    var menuId: Int?,

    var count: Int = 0,

    ) : BaseEntity() {
    fun increaseCount(addCount: Int) {
        this.count += addCount
    }

    fun toMenuOrderStatics(): MenuOrderStatics =
        MenuOrderStatics(
            id = requireNotNull(this.id),
            date = requireNotNull(this.date),
            menuId = requireNotNull(this.menuId),
            count = requireNotNull(this.count),
            createdBy = requireNotNull(this.createdBy),
            createdAt = requireNotNull(this.createdAt),
            modifiedBy = requireNotNull(this.modifiedBy),
            modifiedAt = requireNotNull(this.modifiedAt),
        )
}