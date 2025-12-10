package order.common

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.hibernate.Session
import org.springframework.stereotype.Component

@Aspect
@Component
class SoftDeleteFilterAspect(
    @PersistenceContext
    private val entityManager: EntityManager,
) {
    @Before(
        "@within(org.springframework.transaction.annotation.Transactional) || " +
                "@annotation(org.springframework.transaction.annotation.Transactional)"
    )
    fun enableDeletedFilter() {
        val session = entityManager.unwrap(Session::class.java)
        session.enableFilter("deletedFilter")
    }
}