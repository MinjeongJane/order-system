package order.infrastructure.menu

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import order.domain.menu.Menu
import order.infrastructure.common.BaseEntity
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where

@Entity
@Table(name = "menu", schema = "order_system")
@SQLDelete(sql = "UPDATE menu SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
class MenuEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val name: String? = null,

    val price: Int? = null,
) : BaseEntity() {
    fun toMenu(): Menu =
        Menu(
            id = requireNotNull(this.id),
            name = requireNotNull(this.name),
            price = requireNotNull(this.price)
        )
}