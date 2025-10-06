package order.infrastructure.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class BaseEntity {
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    open var createdBy: String? = null

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    open var createdAt: LocalDateTime? = null

    @LastModifiedBy
    @Column(name = "modified_by", nullable = false)
    open var modifiedBy: String? = null

    @LastModifiedDate
    @Column(name = "modified_at")
    open var modifiedAt: LocalDateTime? = null

    open var deleted: Boolean = false
}