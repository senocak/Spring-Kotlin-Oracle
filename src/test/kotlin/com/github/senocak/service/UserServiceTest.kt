package com.github.senocak.service

import com.github.senocak.createTestUser
import com.github.senocak.domain.Role
import com.github.senocak.domain.User
import com.github.senocak.domain.UserRepository
import com.github.senocak.util.RoleName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mockito.anyList
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyArray
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.mockingDetails
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.sql.ResultSet
import javax.sql.DataSource
import org.springframework.security.core.userdetails.User as SecurityUser

@Tag("unit")
@ExtendWith(MockitoExtension::class)
@DisplayName("Unit Tests for UserService")
class UserServiceTest {
    @InjectMocks
    private lateinit var userService: UserService
    private val userRepository: UserRepository = mock<UserRepository>()
    private val auth: Authentication = mock<Authentication>()
    private val securityUser: SecurityUser = mock<SecurityUser>()
    private val dataSource: DataSource = mock<DataSource>()
    private val cacheService: CacheService = mock<CacheService>()
    private val jdbcClient: JdbcClient = mock<JdbcClient>()
    private val jdbcTemplate: JdbcTemplate = mock<JdbcTemplate>()

    private val userCacheKeyPrefix = "user:"
    private val email = "test@example.com"
    private val user: User = createTestUser()

    @BeforeEach
    fun setup() {
        assertTrue(mockingDetails(userRepository).isMock)
        assertTrue(mockingDetails(cacheService).isMock)
        assertTrue(mockingDetails(jdbcClient).isMock)
        assertTrue(mockingDetails(jdbcTemplate).isMock)
        userService.jdbcTemplate = jdbcTemplate
    }

    @Test
    fun givenUsername_whenExistsByEmail_thenAssertResultIsFalse() {
        // When
        val existsByEmail: Boolean = userService.existsByEmail(email = "username")
        // Then
        assertFalse(existsByEmail)
    }

    @Test
    fun givenUsername_whenExistsByEmail_thenAssertResultIsTrue() {
        // Given
        whenever(methodCall = userRepository.existsByEmail(email = "username")).thenReturn(true)
        // When
        val existsByEmail: Boolean = userService.existsByEmail(email = "username")
        // Then
        assertTrue(existsByEmail)
    }

    @Test
    fun `findByEmail should return user from cache on cache hit`() {
        // Given
        `when`(cacheService.getOrSet(
            eq(email),
            eq(User::class.java),
            eq(userCacheKeyPrefix),
            eq(3600L),
            any()
        )).thenReturn(user)
        // When
        val result: User = userService.findByEmail(email)
        // Then
        assertEquals(user, result)
    }

    @Test
    fun `should invalidate cache when user is saved`() {
        // Given
        whenever(methodCall = userRepository.save(user)).thenReturn(user)
        // When
        val result: User = userService.save(user = user)
        // Then
        verify(cacheService).invalidate(
            key = user.email!!,
            prefix = "user:"
        )
        assertEquals(user, result)
    }

    @Test
    fun `should clear all user caches when deleting all users`() {
        // When
        userService.deleteAllUsers()
        // Then
        verify(userRepository).deleteAll()
        verify(cacheService).invalidatePattern("$userCacheKeyPrefix*")
    }

    @Test
    fun givenEmail_whenLoadUserByUsername_thenAssertResult() {
        // Given
        `when`(cacheService.getOrSet(
            eq(email),
            eq(User::class.java),
            eq(userCacheKeyPrefix),
            eq(3600L),
            any()
        )).thenReturn(user)
        // When
        val result: org.springframework.security.core.userdetails.User = userService.loadUserByUsername(email = email)
        // Then
        assertEquals(user.email, result.username)
        assertEquals(user.password, result.password)
        assertEquals(user.roles
            .map { r: Role -> SimpleGrantedAuthority(RoleName.fromString(r = r.name.toString()).name) }
            .toList().sortedBy { it.authority }, result.authorities.sortedBy { it.authority })
    }

    @Test
    fun `should return users with JdbcTemplate`() {
        // Given
        whenever(methodCall = jdbcTemplate.queryForObject(anyString(), anyArray(), eq(Long::class.java))).thenReturn(2)
        // When
        val result: Page<User> = userService.getUserByTemplate(page = 0, size = 10, name = "q",
            email = "q", roleIds = listOf("1"),)
        // Then
        assertEquals(2, result.totalElements)
        assertEquals(0, result.content.size)
    }

    @Test
    fun `should return users with JdbcClient`() {
        // Given
        val statementSpec: JdbcClient.StatementSpec = mock<JdbcClient.StatementSpec>()
        val mappedQuerySpec: JdbcClient.MappedQuerySpec<Long> = mock<JdbcClient.MappedQuerySpec<Long>>()
        whenever(methodCall = jdbcClient.sql(anyString())).thenReturn(statementSpec)
        whenever(methodCall = statementSpec.params(anyList<Any>())).thenReturn(statementSpec)
        whenever(methodCall = statementSpec.query(Long::class.java)).thenReturn(mappedQuerySpec)
        val mappedQuerySpecUsers = mock<JdbcClient.MappedQuerySpec<JdbcClient.MappedQuerySpec<Long>>>()
        whenever(methodCall = statementSpec.query { _: ResultSet, _: Int -> mappedQuerySpec }).thenReturn(mappedQuerySpecUsers)
        whenever(methodCall = mappedQuerySpec.single()).thenReturn(2)
        whenever(methodCall = mappedQuerySpec.list()).thenReturn(listOf())
        // When
        val result: Page<User> = userService.getUsersWithJdbcClient(page = 0, size = 10, name = "q",
            email = "q", roleIds = listOf("1"))
        // Then
        assertEquals(2, result.totalElements)
        assertEquals(0, result.content.size)
    }
}
