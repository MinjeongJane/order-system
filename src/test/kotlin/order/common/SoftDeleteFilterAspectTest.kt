package order.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SoftDeleteFilterAspectTest {

    @Test
    fun `세션이_있으면_삭제_필터를_활성화한다`() {
        val entityManager = mockk<EntityManager>()
        val session = mockk<Session>(relaxed = true)
        every { entityManager.unwrap(Session::class.java) } returns session

        val aspect = SoftDeleteFilterAspect(entityManager)
        aspect.enableDeletedFilter()

        verify { session.enableFilter("deletedFilter") }
    }

    @Test
    fun `unwrap_실패하면_예외를_전파한다`() {
        val entityManager = mockk<EntityManager>()
        every { entityManager.unwrap(Session::class.java) } throws RuntimeException("unwrap failed")

        val aspect = SoftDeleteFilterAspect(entityManager)

        assertThrows(RuntimeException::class.java) { aspect.enableDeletedFilter() }
    }
}