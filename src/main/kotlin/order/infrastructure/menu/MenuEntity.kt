package order.infrastructure.menu

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import order.domain.menu.Menu
import order.infrastructure.common.BaseEntity
import org.hibernate.annotations.SQLDelete

@Entity
@Table(name = "menu", schema = "order_system")
@SQLDelete(sql = "UPDATE menu SET deleted = true WHERE id = ?")
class MenuEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var name: String,

    var price: Int
) : BaseEntity() {
    fun toMenu(): Menu =
        Menu(
            id = requireNotNull(id),
            name = (name),
            price = (price),
        )
}