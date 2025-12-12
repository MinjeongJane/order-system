package order.infrastructure.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
@FilterDef(name = "deletedFilter")
@Filter(name = "deletedFilter", condition = "deleted = false")
class BaseEntity {
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    var createdBy: String? = null

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null

    @LastModifiedBy
    @Column(name = "modified_by", nullable = false)
    var modifiedBy: String? = null

    @LastModifiedDate
    @Column(name = "modified_at")
    var modifiedAt: LocalDateTime? = null

    var deleted: Boolean = false
}