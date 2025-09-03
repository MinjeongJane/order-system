package order.infrastructure.order

import order.domain.order.OrderHistory
import order.infrastructure.common.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where

@Entity
@Table(name = "order_history", schema = "order_system")
@SQLDelete(sql = "UPDATE order_history SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
class OrderHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long? = null,

    @Column(name = "price", nullable = false)
    val price: Int = 0,
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
            userId = requireNotNull(this.userId),
            price = requireNotNull(this.price),
            createdDate = requireNotNull(this.createdDate),
            updatedDate = requireNotNull(this.updatedDate),
        )
}