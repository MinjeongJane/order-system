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

@Entity
@Table(name = "order_details", schema = "order_system")
@SQLDelete(sql = "UPDATE order_details SET deleted = true WHERE id = ?")
class OrderDetailsEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var orderHistory: OrderHistoryEntity,

    @Column(name = "menu_id", nullable = false)
    var menuId: Int,

    @Column(nullable = false)
    var count: Int = 0,

    @Column(name = "menu_price", nullable = false)
    var menuPrice: Int = 0,
) : BaseEntity() {
    fun toOrderDetails(): OrderDetails =
        OrderDetails(
            id = requireNotNull(this.id),
            orderId = requireNotNull(this.orderHistory.toOrderHistory()),
            menuId = this.menuId,
            count = requireNotNull(this.count),
            menuPrice = requireNotNull(this.menuPrice),
            createdBy = requireNotNull(this.createdBy),
            createdAt = requireNotNull(this.createdAt),
            modifiedBy = requireNotNull(this.modifiedBy),
            modifiedAt = requireNotNull(this.modifiedAt),
        )
}