package order.infrastructure.user

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import order.domain.user.UserCredit
import order.infrastructure.common.BaseEntity
import org.hibernate.annotations.SQLDelete

@Entity
@Table(name = "`USER_CREDIT`", schema = "order_system")
@SQLDelete(sql = "UPDATE `USER_CREDIT` SET deleted = true WHERE id = ?")
class UserCreditEntity(
    @Id
    var id: Long? = null,

    var credits: Int = 0,
) : BaseEntity() {
    fun toUser(): UserCredit = UserCredit(
        id = requireNotNull(this.id),
        credits = this.credits,
    )
}
