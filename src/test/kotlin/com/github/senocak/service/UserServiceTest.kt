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
import org.mockito.kotlin.doReturn
import javax.sql.DataSource
import org.springframework.security.core.userdetails.User as SecurityUser

@Tag("unit")
@ExtendWith(MockitoExtension::class)
@DisplayName("Unit Tests for UserService")
class UserServiceTest {
    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private var auth: Authentication = Mockito.mock(Authentication::class.java)
    private var securityUser: SecurityUser = Mockito.mock(SecurityUser::class.java)
    private var dataSource: DataSource = Mockito.mock(DataSource::class.java)
    private val userService: UserService = UserService(userRepository = userRepository, dataSource = dataSource)

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
        doReturn(value = user).`when`(userRepository).findByEmail(email = "email")
        // When
        val findByUsername: User = userService.findByEmail(email = "email")
        // Then
        assertEquals(user, findByUsername)
    }

    @Test
    fun givenNullEmail_whenFindByEmail_thenAssertResult() {
        // When
        val closureToTest = Executable { userService.findByEmail(email = "email") }
        // Then
        assertThrows(UsernameNotFoundException::class.java, closureToTest)
    }

    @Test
    fun givenUser_whenSave_thenAssertResult() {
        // Given
        val user: User = createTestUser()
        doReturn(value = user).`when`(userRepository).save<User>(user)
        // When
        val save: User = userService.save(user)
        // Then
        assertEquals(user, save)
    }

    @Test
    fun givenUser_whenCreate_thenAssertResult() {
        // Given
        val user: User = createTestUser()
        `when`(userRepository.save(user)).thenReturn(user)
        // When
        val create: User = userService.save(user)
        // Then
        assertEquals(user, create)
    }

    @Test
    fun givenNullUsername_whenLoadUserByUsername_thenAssertResult() {
        // When
        val closureToTest = Executable { userService.loadUserByUsername("username") }
        // Then
        assertThrows(UsernameNotFoundException::class.java, closureToTest)
    }

    @Test
    fun givenUsername_whenLoadUserByUsername_thenAssertResult() {
        // Given
        val user: User = createTestUser()
        doReturn(value = user).`when`(userRepository).findByEmail(email = "email")
        // When
        val loadUserByUsername: org.springframework.security.core.userdetails.User = userService.loadUserByUsername(email = "email")
        // Then
        assertEquals(user.email, loadUserByUsername.username)
    }

    @Test
    fun givenNotLoggedIn_whenLoadUserByUsername_thenAssertResult() {
        // Given
        SecurityContextHolder.getContext().authentication = auth
        doReturn(value = securityUser).`when`(auth).principal
        doReturn(value = "user").`when`(securityUser).username
        // When
        val closureToTest = Executable { userService.loggedInUser() }
        // Then
        assertThrows(UsernameNotFoundException::class.java, closureToTest)
    }

    @Test
    @Throws(UsernameNotFoundException::class)
    fun givenLoggedIn_whenLoadUserByUsername_thenAssertResult() {
        // Given
        SecurityContextHolder.getContext().authentication = auth
        doReturn(value = securityUser).`when`(auth).principal
        doReturn(value = "email").`when`(securityUser).username
        val user: User = createTestUser()
        doReturn(value = user).`when`(userRepository).findByEmail(email = "email")
        // When
        val loggedInUser: User = userService.loggedInUser()
        // Then
        assertEquals(user.email, loggedInUser.email)
    }
}