package order.infrastructure.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class BaseEntity {
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    open var createdDate: LocalDateTime? = null

    @LastModifiedDate
    @Column(name = "updated_date")
    open var updatedDate: LocalDateTime? = null

    open var deleted: Boolean = false
}