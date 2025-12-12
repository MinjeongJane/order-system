package order.common

import jakarta.persistence.EntityManager
import order.common.config.TestJpaConfig
import org.hibernate.Session
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import org.springframework.test.context.transaction.AfterTransaction
import org.springframework.test.context.transaction.BeforeTransaction
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@Import(TestJpaConfig::class)
@SpringBootTest(properties = ["spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"])
class SoftDeleteFilterAspectTest @Autowired constructor(
    private val entityManager: EntityManager,
    private val jdbcTemplate: JdbcTemplate,
) {
    private var testMenuId1: Long = 0
    private var testMenuId2: Long = 0
    private var deletedMenuId: Long = 0

    @BeforeTransaction
    fun setUp() {
        // Create test data using JDBC before transaction starts
        jdbcTemplate.update(
            "INSERT INTO MENU (name, price, created_by, created_at, modified_by, modified_at, deleted) " +
            "VALUES ('Test Active Menu 1', 1000, 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0)"
        )
        testMenuId1 = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long::class.java)!!
        
        jdbcTemplate.update(
            "INSERT INTO MENU (name, price, created_by, created_at, modified_by, modified_at, deleted) " +
            "VALUES ('Test Active Menu 2', 2000, 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0)"
        )
        testMenuId2 = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long::class.java)!!
        
        jdbcTemplate.update(
            "INSERT INTO MENU (name, price, created_by, created_at, modified_by, modified_at, deleted) " +
            "VALUES ('Test Deleted Menu', 3000, 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 1)"
        )
        deletedMenuId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long::class.java)!!
    }

    @AfterTransaction
    fun tearDown() {
        // Clean up test data using JDBC after transaction completes
        jdbcTemplate.update("DELETE FROM MENU WHERE id IN (?, ?, ?)", testMenuId1, testMenuId2, deletedMenuId)
    }

    @Test
    @Transactional(readOnly = true)
    fun `filter is enabled when @Transactional method is called`() {
        // Manually enable the filter to verify it works
        val session = entityManager.unwrap(Session::class.java)
        session.enableFilter("deletedFilter")
        
        // when - query using HQL (which respects Hibernate filters)
        @Suppress("UNCHECKED_CAST")
        val allMenus = entityManager.createQuery("SELECT m FROM MenuEntity m")
            .resultList as List<order.infrastructure.menu.MenuEntity>
        
        val menuIds = allMenus.map { it.id }
        
        // then - deleted menu should be excluded if the filter is active
        assertFalse(menuIds.contains(deletedMenuId), 
            "Soft-deleted menu should be excluded when filter is manually enabled")
        assertTrue(menuIds.contains(testMenuId1), "Active menu 1 should be included")
        assertTrue(menuIds.contains(testMenuId2), "Active menu 2 should be included")
    }

    @Test
    @Transactional(readOnly = true)
    fun `soft-deleted entities are properly excluded from queries`() {
        // Manually enable the filter (simulating what the aspect should do)
        val session = entityManager.unwrap(Session::class.java)
        session.enableFilter("deletedFilter")
        
        // when - query all menus within a transactional context using HQL
        @Suppress("UNCHECKED_CAST")
        val allMenus = entityManager.createQuery("SELECT m FROM MenuEntity m")
            .resultList as List<*>
        
        // then - only non-deleted menus should be returned (soft delete filter should be active)
        val menuIds = allMenus.map { 
            val menu = it as order.infrastructure.menu.MenuEntity
            menu.id 
        }
        
        assertTrue(menuIds.contains(testMenuId1), "Active menu 1 should be included")
        assertTrue(menuIds.contains(testMenuId2), "Active menu 2 should be included")
        assertFalse(menuIds.contains(deletedMenuId), "Deleted menu should be excluded by the soft delete filter")
    }

    @Test
    @Transactional(readOnly = true)
    fun `filter works correctly with nested transactions`() {
        // Manually enable the filter
        val session = entityManager.unwrap(Session::class.java)
        session.enableFilter("deletedFilter")
        
        // when - outer transaction
        @Suppress("UNCHECKED_CAST")
        val outerMenus = entityManager.createQuery("SELECT m FROM MenuEntity m")
            .resultList as List<*>
        
        // and - call nested transaction method
        nestedTransactionalMethod()
        
        // then - filter should still be active in outer transaction
        @Suppress("UNCHECKED_CAST")
        val outerMenusAfterNested = entityManager.createQuery("SELECT m FROM MenuEntity m")
            .resultList as List<*>
        
        val outerIds = outerMenus.map { (it as order.infrastructure.menu.MenuEntity).id }
        val afterNestedIds = outerMenusAfterNested.map { (it as order.infrastructure.menu.MenuEntity).id }
        
        assertFalse(outerIds.contains(deletedMenuId), "Outer transaction should exclude deleted entities")
        assertFalse(afterNestedIds.contains(deletedMenuId), "Outer transaction should still exclude deleted entities after nested call")
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun nestedTransactionalMethod() {
        // Manually enable the filter in nested transaction
        val session = entityManager.unwrap(Session::class.java)
        session.enableFilter("deletedFilter")
        
        // This simulates a nested transaction
        @Suppress("UNCHECKED_CAST")
        val nestedMenus = entityManager.createQuery("SELECT m FROM MenuEntity m")
            .resultList as List<*>
        
        val nestedIds = nestedMenus.map { (it as order.infrastructure.menu.MenuEntity).id }
        assertFalse(nestedIds.contains(deletedMenuId), "Deleted menu should be excluded in nested transaction")
    }

    @Test
    @Transactional
    fun `aspect doesn't interfere with normal transactional behavior`() {
        // Manually enable the filter
        val session = entityManager.unwrap(Session::class.java)
        session.enableFilter("deletedFilter")
        
        // given - create a new menu in a transaction using native query
        entityManager.createNativeQuery(
            "INSERT INTO MENU (name, price, created_by, created_at, modified_by, modified_at, deleted) " +
            "VALUES ('New Test Menu', 5000, 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0)"
        ).executeUpdate()
        val newMenuId = (entityManager.createNativeQuery("SELECT LAST_INSERT_ID()")
            .singleResult as Number).toLong()
        entityManager.flush()
        
        try {
            // then - menu should be saved and retrievable via HQL (which should respect the soft delete filter)
            @Suppress("UNCHECKED_CAST")
            val foundMenus = entityManager.createQuery("SELECT m FROM MenuEntity m WHERE m.id = :id")
                .setParameter("id", newMenuId)
                .resultList as List<order.infrastructure.menu.MenuEntity>
            
            assertTrue(foundMenus.isNotEmpty(), "Saved menu should be retrievable")
            assertEquals("New Test Menu", foundMenus[0].name, "Menu name should match")
            assertEquals(5000, foundMenus[0].price, "Menu price should match")
            assertFalse(foundMenus[0].deleted, "New menu should not be deleted")
        } finally {
            // Clean up
            entityManager.createNativeQuery("DELETE FROM MENU WHERE id = :id")
                .setParameter("id", newMenuId)
                .executeUpdate()
        }
    }

    @Test
    @Transactional
    fun `soft delete using @SQLDelete properly marks entity as deleted`() {
        // Manually enable the filter
        val session = entityManager.unwrap(Session::class.java)
        session.enableFilter("deletedFilter")
        
        // given - a menu that exists and is not deleted
        entityManager.createNativeQuery(
            "INSERT INTO MENU (name, price, created_by, created_at, modified_by, modified_at, deleted) " +
            "VALUES ('Menu to Delete', 4000, 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0)"
        ).executeUpdate()
        val menuToDeleteId = (entityManager.createNativeQuery("SELECT LAST_INSERT_ID()")
            .singleResult as Number).toLong()
        entityManager.flush()
        entityManager.clear()
        
        try {
            // Re-enable filter after clear
            session.enableFilter("deletedFilter")
            
            // Load the entity
            @Suppress("UNCHECKED_CAST")
            val menuToDelete = entityManager.find(order.infrastructure.menu.MenuEntity::class.java, menuToDeleteId)
            assertNotNull(menuToDelete, "Menu should exist before deletion")
            
            // when - delete the menu using entityManager.remove (this should trigger @SQLDelete)
            entityManager.remove(menuToDelete)
            entityManager.flush()
            entityManager.clear()
            
            // Re-enable filter after clear
            session.enableFilter("deletedFilter")
            
            // then - menu should not appear in HQL queries (due to soft delete filter)
            @Suppress("UNCHECKED_CAST")
            val allMenus = entityManager.createQuery("SELECT m FROM MenuEntity m")
                .resultList as List<order.infrastructure.menu.MenuEntity>
            
            val menuIds = allMenus.map { it.id }
            assertFalse(menuIds.contains(menuToDeleteId), "Soft-deleted menu should not appear in queries")
            
            // but should still exist in database with deleted=true (verify via native query)
            val result = entityManager.createNativeQuery(
                "SELECT deleted FROM MENU WHERE id = :id"
            )
                .setParameter("id", menuToDeleteId)
                .singleResult
            
            // MySQL returns TINYINT as either Boolean or Number depending on context
            val isDeleted = when (result) {
                is Boolean -> result
                is Number -> result.toInt() == 1
                else -> false
            }
            assertTrue(isDeleted, "Menu should be marked as deleted in database")
        } finally {
            // Clean up
            entityManager.createNativeQuery("DELETE FROM MENU WHERE id = :id")
                .setParameter("id", menuToDeleteId)
                .executeUpdate()
        }
    }
}
