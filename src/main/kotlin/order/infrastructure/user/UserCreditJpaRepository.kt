package order.infrastructure.user

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface UserCreditJpaRepository : JpaRepository<UserCreditEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserCreditEntity u WHERE u.id = :id")
    fun findByIdWithLock(id: Long): UserCreditEntity?
}