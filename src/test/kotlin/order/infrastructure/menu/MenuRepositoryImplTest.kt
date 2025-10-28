package order.infrastructure.menu

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.confirmVerified
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessResourceFailureException
import order.api.dto.MenuRequest

class MenuRepositoryImplTest {

    private val menuJpaRepository: MenuJpaRepository = mockk()
    private val menuRepositoryImpl = MenuRepositoryImpl(menuJpaRepository)

    @Test
    fun `메뉴저장 성공`() {
        val menuReq = mockk<MenuRequest>()
        every { menuReq.id } returns 1L
        every { menuReq.name } returns "MenuA"
        every { menuReq.price } returns 1000

        val savedEntity = MenuEntity(id = 1L, name = "MenuA", price = 1000)
        every { menuJpaRepository.saveAll(any<List<MenuEntity>>()) } returns listOf(savedEntity)

        assertDoesNotThrow { menuRepositoryImpl.saveMenu(listOf(menuReq)) }

        verify(exactly = 1) { menuJpaRepository.saveAll(any<List<MenuEntity>>()) }
        confirmVerified(menuJpaRepository)
    }

    @Test
    fun `메뉴저장 결과가 empty면 DataAccessResourceFailureException 발생`() {
        val menuReq = mockk<MenuRequest>()
        every { menuReq.id } returns 1L
        every { menuReq.name } returns "MenuA"
        every { menuReq.price } returns 1000

        every { menuJpaRepository.saveAll(any<List<MenuEntity>>()) } returns emptyList()

        assertThrows(DataAccessResourceFailureException::class.java) {
            menuRepositoryImpl.saveMenu(listOf(menuReq))
        }

        verify(exactly = 1) { menuJpaRepository.saveAll(any<List<MenuEntity>>()) }
        confirmVerified(menuJpaRepository)
    }
}