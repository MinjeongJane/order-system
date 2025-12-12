package order.infrastructure.order

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import order.domain.order.OrderHistory
import order.infrastructure.common.BaseEntity
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.SQLDelete

@Entity
@Table(name = "`ORDER_HISTORY`", schema = "order_system")
@SQLDelete(sql = "UPDATE `ORDER_HISTORY` SET deleted = true WHERE id = ?")
class OrderHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "price", nullable = false)
    var price: Int = 0,
) : BaseEntity() {
    @OneToMany(
        mappedBy = "orderHistory",
        cascade = [CascadeType.PERSIST, CascadeType.MERGE],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @BatchSize(size = 20)
    val orderDetails: MutableList<OrderDetailsEntity> = mutableListOf()

    fun addDetails(menuId: Int, count: Int, menuPrice: Int) {
        val detail = OrderDetailsEntity(
            orderHistory = this,
            menuId = menuId,
            count = count,
            menuPrice = menuPrice
        )
        orderDetails += detail
    }

    fun toOrderHistory(): OrderHistory =
        OrderHistory(
            id = requireNotNull(this.id),
            userId = this.userId,
            price = this.price,
            createdBy = requireNotNull(this.createdBy),
            createdAt = requireNotNull(this.createdAt),
            modifiedBy = requireNotNull(this.modifiedBy),
            modifiedAt = requireNotNull(this.modifiedAt)
        )
}