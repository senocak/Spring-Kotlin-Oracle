package com.github.senocak.service

import com.github.senocak.createTestUser
import com.github.senocak.domain.User
import com.github.senocak.domain.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.never
import org.mockito.kotlin.doReturn
import org.mockito.ArgumentMatchers.eq
import javax.sql.DataSource
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.RowMapper
import org.springframework.security.core.userdetails.User as SecurityUser

@Tag("unit")
@ExtendWith(MockitoExtension::class)
@DisplayName("Unit Tests for UserService")
class UserServiceTest {
    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private var auth: Authentication = Mockito.mock(Authentication::class.java)
    private var securityUser: SecurityUser = Mockito.mock(SecurityUser::class.java)
    private var dataSource: DataSource = Mockito.mock(DataSource::class.java)
    private val cacheService: CacheService = Mockito.mock(CacheService::class.java)
    private val jdbcClient: JdbcClient = Mockito.mock(JdbcClient::class.java)
    private val userService: UserService = UserService(
        userRepository = userRepository,
        dataSource = dataSource,
        cacheService = cacheService,
        jdbcClient = jdbcClient
    )

    private val user: User = createTestUser()

    @Test
    fun givenUsername_whenExistsByEmail_thenAssertResult() {
        // When
        val existsByEmail: Boolean = userService.existsByEmail("username")
        // Then
        assertFalse(existsByEmail)
    }

    @Test
    fun givenEmail_whenFindByEmail_thenAssertResult() {
        // Given
        val user: User = createTestUser()
        doReturn(user)
            .`when`(cacheService)
            .getOrSet(any(), any(), any(), any())
        // When
        val findByUsername: User = userService.findByEmail(email = "email")
        // Then
        assertEquals(user, findByUsername)
    }

    @Test
    fun givenNullEmail_whenFindByEmail_thenAssertResult() {
        // Given
        whenever(cacheService.getOrSet(
            key = eq("email"),
            prefix = eq("user:"),
            clazz = eq(User::class.java),
            loader = any()
        )).thenReturn(null)
        // When
        val closureToTest = Executable { userService.findByEmail(email = "email") }
        // Then
        assertThrows(UsernameNotFoundException::class.java, closureToTest)
    }

    @Test
    fun givenUser_whenSave_thenAssertResult() {
        // Given
        val user: User = createTestUser()
        whenever(userRepository.save(user)).thenReturn(user)
        // When
        val save: User = userService.save(user)
        // Then
        assertEquals(user, save)
    }

    @Test
    fun givenUser_whenCreate_thenAssertResult() {
        // Given
        val user: User = createTestUser()
        whenever(userRepository.save(user)).thenReturn(user)
        // When
        val create: User = userService.save(user)
        // Then
        assertEquals(user, create)
    }

    @Test
    fun givenNullUsername_whenLoadUserByUsername_thenAssertResult() {
        // Given
        whenever(cacheService.getOrSet(
            key = eq("username"),
            prefix = eq("user:"),
            clazz = eq(User::class.java),
            loader = any()
        )).thenReturn(null)
        // When
        val closureToTest = Executable { userService.loadUserByUsername("username") }
        // Then
        assertThrows(UsernameNotFoundException::class.java, closureToTest)
    }

    @Test
    fun givenUsername_whenLoadUserByUsername_thenAssertResult() {
        // Given
        val user: User = createTestUser()
        doReturn(user)
            .`when`(cacheService)
            .getOrSet<User>(
                key = any(),
                prefix = any(),
                clazz = any(),
                loader = any()
            )
        // When
        val loadUserByUsername: org.springframework.security.core.userdetails.User = userService.loadUserByUsername(email = "email")
        // Then
        assertEquals(user.email, loadUserByUsername.username)
    }

    @Test
    fun givenNotLoggedIn_whenLoadUserByUsername_thenAssertResult() {
        // Given
        SecurityContextHolder.getContext().authentication = auth
        whenever(auth.principal).thenReturn(securityUser)
        whenever(securityUser.username).thenReturn("user")
        whenever(cacheService.getOrSet(
            key = eq("user"),
            prefix = eq("user:"),
            clazz = eq(User::class.java),
            loader = any()
        )).thenReturn(null)
        // When
        val closureToTest = Executable { userService.loggedInUser() }
        // Then
        assertThrows(UsernameNotFoundException::class.java, closureToTest)
    }

    @Test
    fun givenLoggedIn_whenLoadUserByUsername_thenAssertResult() {
        // Given
        SecurityContextHolder.getContext().authentication = auth
        whenever(auth.principal).thenReturn(securityUser)
        whenever(securityUser.username).thenReturn("email")
        val user: User = createTestUser()
        whenever(cacheService.getOrSet(
            key = eq("email"),
            prefix = eq("user:"),
            clazz = eq(User::class.java),
            loader = any()
        )).thenReturn(user)
        // When
        val loggedInUser: User = userService.loggedInUser()
        // Then
        assertEquals(user.email, loggedInUser.email)
    }

    @Test
    fun `should return user from cache when available`() {
        // Given
        val email = "test@example.com"
        val user = createTestUser()
        doReturn(user).`when`(cacheService).getOrSet(
            key = email,
            prefix = "user:",
            clazz = User::class.java,
            loader = any()
        )

        // When
        val result = userService.findByEmail(email)

        // Then
        assertEquals(user, result)
        verify(userRepository, never()).findByEmail(email)
    }

    @Test
    fun `should invalidate cache when user is saved`() {
        // Given
        val user = createTestUser()
        doReturn(user).`when`(userRepository).save(user)

        // When
        userService.save(user)

        // Then
        verify(cacheService).invalidate(
            key = user.email!!,
            prefix = "user:"
        )
    }

    @Test
    fun `should clear all user caches when deleting all users`() {
        // When
        userService.deleteAllUsers()

        // Then
        verify(userRepository).deleteAll()
        verify(cacheService).invalidatePattern("user:*")
    }

    @Test
    fun `should return paginated users with JdbcClient`() {
        // Given
        val testUser = createTestUser()
        val page = 0
        val size = 10

        // Mock the count query
        val mockStatementSpec = Mockito.mock(JdbcClient.StatementSpec::class.java)
        doReturn(mockStatementSpec).`when`(jdbcClient).sql(any())
        doReturn(mockStatementSpec).`when`(mockStatementSpec).params(any<Array<Any>>())
        doReturn(1L).`when`(mockStatementSpec).query(Long::class.java).single()

        // Mock the main query
        doReturn(listOf(testUser)).`when`(mockStatementSpec).query(any<RowMapper<User>>()).list()

        // When
        val result = userService.getUsersWithJdbcClient(
            page = page,
            size = size,
            name = null,
            email = null,
            roleIds = null
        )

        // Then
        assertEquals(1, result.totalElements)
        assertEquals(1, result.content.size)
        assertEquals(testUser, result.content[0])
    }

    @Test
    fun `should return filtered users with JdbcClient`() {
        // Given
        val testUser = createTestUser()
        val page = 0
        val size = 10
        val searchTerm = "test"

        // Mock the count query
        val mockStatementSpec = Mockito.mock(JdbcClient.StatementSpec::class.java)
        doReturn(mockStatementSpec).`when`(jdbcClient).sql(any())
        doReturn(mockStatementSpec).`when`(mockStatementSpec).params(any<Array<Any>>())
        doReturn(1L).`when`(mockStatementSpec).query(Long::class.java).single()

        // Mock the main query
        doReturn(listOf(testUser)).`when`(mockStatementSpec).query(any<RowMapper<User>>()).list()

        // When
        val result = userService.getUsersWithJdbcClient(
            page = page,
            size = size,
            name = searchTerm,
            email = searchTerm,
            roleIds = null
        )

        // Then
        assertEquals(1, result.totalElements)
        assertEquals(1, result.content.size)
        assertEquals(testUser, result.content[0])
    }
}
