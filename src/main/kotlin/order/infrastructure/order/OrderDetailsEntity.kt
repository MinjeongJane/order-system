package order.infrastructure.order

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import order.domain.order.OrderDetails
import order.infrastructure.common.BaseEntity
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where

@Entity
@Table(name = "order_details", schema = "order_system")
@SQLDelete(sql = "UPDATE order_details SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
class OrderDetailsEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val orderHistory: OrderHistoryEntity,

    @Column(name = "menu_id", nullable = false)
    val menuId: Int? = null,

    @Column(nullable = false)
    val count: Int = 0,

    @Column(name = "menu_price", nullable = false)
    val menuPrice: Int = 0,
) : BaseEntity() {
    fun toOrderDetails(): OrderDetails =
        OrderDetails(
            id = requireNotNull(this.id),
            orderId = requireNotNull(this.orderHistory.toOrderHistory()),
            menuId = requireNotNull(this.menuId),
            count = requireNotNull(this.count),
            menuPrice = requireNotNull(this.menuPrice),
            createdDate = requireNotNull(this.createdDate),
            updatedDate = requireNotNull(this.updatedDate),
        )
}