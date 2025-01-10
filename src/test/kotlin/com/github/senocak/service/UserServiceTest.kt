package com.github.senocak.service

import com.github.senocak.createTestTodo
import com.github.senocak.createTestUser
import com.github.senocak.domain.TodoItem
import com.github.senocak.domain.TodoItemRepository
import com.github.senocak.domain.User
import com.github.senocak.domain.UserRepository
import com.github.senocak.domain.dto.CreateTodoDto
import com.github.senocak.domain.dto.UpdateTodoDto
import com.github.senocak.exception.ServerException
import java.util.Optional
import java.util.UUID
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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.core.userdetails.User as SecurityUser

@Tag("unit")
@ExtendWith(MockitoExtension::class)
@DisplayName("Unit Tests for UserService")
class UserServiceTest {
    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val todoItemRepository: TodoItemRepository = Mockito.mock(TodoItemRepository::class.java)
    private var auth: Authentication = Mockito.mock(Authentication::class.java)
    private var securityUser: SecurityUser = Mockito.mock(SecurityUser::class.java)
    private val userService: UserService = UserService(userRepository = userRepository, todoItemRepository = todoItemRepository)

    private val user: User = createTestUser()
    private val todo: TodoItem = createTestTodo()

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

    @Test
    fun givenIdAndPageable_whenFindByTodoItems_thenAssertResult() {
        // Given
        val id: UUID = UUID.randomUUID()
        val pageable: Pageable = Pageable.unpaged()
        val pages: PageImpl<TodoItem> = PageImpl(listOf(element = todo))
        doReturn(value = pages).`when`(todoItemRepository).findAllByOwner(owner = id, pageable = pageable)
        // When
        val response: Page<TodoItem> = userService.findByTodoItems(id = id, pageable = pageable)
        // Then
        assertEquals(1, response.totalPages)
        assertEquals(1, response.totalElements)
        assertEquals(1, response.content.size)
        assertEquals(todo.id, response.content.first().id)
        assertEquals(todo.description, response.content.first().description)
        assertEquals(todo.finished, response.content.first().finished)
    }

    @Test
    fun givenId_whenFindByTodoItem_thenAssertResult() {
        // Given
        val id: UUID = UUID.randomUUID()
        doReturn(value = Optional.of(todo)).`when`(todoItemRepository).findById(id)
        // When
        val response: TodoItem = userService.findTodoItem(id = id)
        // Then
        assertEquals(todo.id, response.id)
        assertEquals(todo.description, response.description)
        assertEquals(todo.finished, response.finished)
    }

    @Test
    fun givenId_whenFindByTodoItem_thenThrowServerException() {
        // Given
        val id: UUID = UUID.randomUUID()
        doReturn(value = Optional.empty<TodoItem>()).`when`(todoItemRepository).findById(id)
        // When
        val response = Executable { userService.findTodoItem(id = id) }
        // Then
        assertThrows(ServerException::class.java, response)
    }

    @Test
    fun givenId_whenCreateTodoItem_thenAssertResult() {
        // Given
        val createTodo = CreateTodoDto(description = "description")
        doReturn(value = todo).`when`(todoItemRepository).save(ArgumentMatchers.any(TodoItem::class.java))
        // When
        val response: TodoItem = userService.createTodoItem(createTodo = createTodo, owner = user)
        // Then
        assertEquals(todo.id, response.id)
        assertEquals(createTodo.description, response.description)
        assertFalse(response.finished)
    }

    @Test
    fun givenId_whenUpdateTodoItem_thenAssertResult() {
        // Given
        val id: UUID = UUID.randomUUID()
        doReturn(value = Optional.of(todo)).`when`(todoItemRepository).findById(id)
        val updateTodoDto = UpdateTodoDto(description = "description", finished = true)
        doReturn(value = todo).`when`(todoItemRepository).save(ArgumentMatchers.any(TodoItem::class.java))
        // When
        val response: TodoItem = userService.updateTodoItem(id = id, updateTodoDto = updateTodoDto)
        // Then
        assertEquals(todo.id, response.id)
        assertEquals(updateTodoDto.description, response.description)
        assertEquals(updateTodoDto.finished, response.finished)
    }

    @Test
    fun givenId_whenDeleteTodoItem_thenAssertResult() {
        // Given
        val id: UUID = UUID.randomUUID()
        doReturn(value = Optional.of(todo)).`when`(todoItemRepository).findById(id)
        // When
        userService.deleteTodoItem(id = id)
        // Then
        verify(todoItemRepository).delete(todo)
    }
}