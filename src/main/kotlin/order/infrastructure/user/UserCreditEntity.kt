package order.infrastructure.user

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import order.domain.user.UserCredit
import order.infrastructure.common.BaseEntity
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where

@Entity
@Table(name = "user_credit", schema = "order_system")
@SQLDelete(sql = "UPDATE user_credit SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
class UserCreditEntity(
    @Id
    val id: Long? = null,

    var credits: Int = 0,
) : BaseEntity() {
    fun toUser(): UserCredit = UserCredit(
        id = requireNotNull(this.id),
        credits = requireNotNull(this.credits),
    )
}
