package com.github.senocak.controller

import com.github.senocak.TestConstants
import com.github.senocak.createTestTodo
import com.github.senocak.createTestUser
import com.github.senocak.domain.TodoItem
import com.github.senocak.domain.User
import com.github.senocak.domain.dto.CreateTodoDto
import com.github.senocak.domain.dto.TodoDto
import com.github.senocak.domain.dto.TodoItemPaginationDTO
import com.github.senocak.domain.dto.UpdateTodoDto
import com.github.senocak.domain.dto.UpdateUserDto
import com.github.senocak.domain.dto.UserResponse
import com.github.senocak.exception.ServerException
import com.github.senocak.service.UserService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.BindingResult
import jakarta.servlet.http.HttpServletRequest
import java.util.UUID
import org.mockito.InjectMocks
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@Tag("unit")
@ExtendWith(MockitoExtension::class)
@DisplayName("Unit Tests for UserController")
class UserControllerTest {
    @InjectMocks lateinit var userController: UserController
    private val userService: UserService = mock<UserService>()
    private val passwordEncoder: PasswordEncoder = mock<PasswordEncoder>()
    private val bindingResult: BindingResult = mock<BindingResult>()
    private val user: User = createTestUser()
    private val todo: TodoItem = createTestTodo()

    @Nested
    internal inner class GetMeTest {
        
        @Test
        @Throws(ServerException::class)
        fun givenServerException_whenGetMe_thenThrowServerException() {
            // Given
            doThrow(toBeThrown = ServerException::class).`when`(userService).loggedInUser()
            // When
            val closureToTest = Executable { userController.me() }
            // Then
            assertThrows(ServerException::class.java, closureToTest)
        }

        @Test
        @Throws(ServerException::class)
        fun given_whenGetMe_thenReturn200() {
            // Given
            doReturn(value = user).`when`(userService).loggedInUser()
            // When
            val getMe: UserResponse = userController.me()
            // Then
            assertNotNull(getMe)
            assertEquals(user.email, getMe.email)
            assertEquals(user.name, getMe.name)
        }
    }

    @Nested
    internal inner class PatchMeTest {
        private val updateUserDto: UpdateUserDto = UpdateUserDto()
        private val httpServletRequest: HttpServletRequest = Mockito.mock(HttpServletRequest::class.java)

        @Test
        @Throws(ServerException::class)
        fun givenNullPasswordConf_whenPatchMe_thenThrowServerException() {
            // Given
            doReturn(value = user).`when`(userService).loggedInUser()
            updateUserDto.password = "pass1"
            // When
            val closureToTest = Executable { userController.patchMe(httpServletRequest, updateUserDto, bindingResult) }
            // Then
            assertThrows(ServerException::class.java, closureToTest)
        }

        @Test
        @Throws(ServerException::class)
        fun givenInvalidPassword_whenPatchMe_thenThrowServerException() {
            // Given
            doReturn(value = user).`when`(userService).loggedInUser()
            updateUserDto.password = "pass1"
            updateUserDto.passwordConfirmation = "pass2"
            // When
            val closureToTest = Executable { userController.patchMe(httpServletRequest, updateUserDto, bindingResult) }
            // Then
            assertThrows(ServerException::class.java, closureToTest)
        }

        @Test
        @Throws(ServerException::class)
        fun given_whenPatchMe_thenThrowServerException() {
            // Given
            doReturn(value = user).`when`(userService).loggedInUser()
            updateUserDto.name = TestConstants.USER_NAME
            updateUserDto.password = "pass1"
            updateUserDto.passwordConfirmation = "pass1"
            doReturn(value = "pass1").`when`(passwordEncoder).encode("pass1")
            doReturn(value = user).`when`(userService).save(user = user)
            // When
            val patchMe: UserResponse = userController.patchMe(httpServletRequest, updateUserDto, bindingResult)
            // Then
            assertNotNull(patchMe)
            assertEquals(user.email, patchMe.email)
            assertEquals(user.name, patchMe.name)
        }
    }

    @Nested
    internal inner class TodosTest {
        @Test
        @Throws(ServerException::class)
        fun given_whenGetTodos_thenReturn200() {
            // Given
            doReturn(value = user).`when`(userService).loggedInUser()
            val pages: PageImpl<TodoItem> = PageImpl(listOf(element = todo))
            doReturn(value = pages).`when`(userService).findByTodoItems(id = user.id!!, pageable = PageRequest.of(0, 100))
            // When
            val response: TodoItemPaginationDTO = userController.todos(page = 0, size = 100)
            // Then
            assertEquals(1, response.pages)
            assertEquals(1, response.page)
            assertEquals(1, response.total)
            assertEquals(1, response.items?.size)
            assertEquals("${todo.id}", response.items?.first()?.id)
            assertEquals(todo.description, response.items?.first()?.description)
            assertEquals(todo.finished, response.items?.first()?.finished)
        }

        @Test
        @Throws(ServerException::class)
        fun given_whenCreateTodo_thenReturn201() {
            // Given
            val createTodo = CreateTodoDto(description = "description")
            val bindingResult: BindingResult = mock<BindingResult>()

            doReturn(value = user).`when`(userService).loggedInUser()
            doReturn(value = todo).`when`(userService).createTodoItem(createTodo = createTodo, owner = user)
            // When
            val response: TodoDto = userController.createTodo(createTodo = createTodo, resultOfValidation = bindingResult)
            // Then
            assertEquals("${todo.id}", response.id)
            assertEquals(createTodo.description, response.description)
            assertFalse(response.finished!!)
        }

        @Test
        @Throws(ServerException::class)
        fun given_whenGetTodo_thenReturn200() {
            // Given
            val id: UUID = UUID.randomUUID()
            doReturn(value = user).`when`(userService).loggedInUser()
            doReturn(value = todo).`when`(userService).findTodoItem(id = id)
            // When
            val response: TodoDto = userController.getTodo(id = "$id")
            // Then
            assertEquals("${todo.id}", response.id)
            assertEquals(todo.description, response.description)
            assertFalse(response.finished!!)
        }

        @Test
        @Throws(ServerException::class)
        fun given_whenUpdateTodo_thenReturn200() {
            // Given
            val id: UUID = UUID.randomUUID()
            val updateTodoDto = UpdateTodoDto(description = "description", finished = true)
            val bindingResult: BindingResult = mock<BindingResult>()
            doReturn(value = user).`when`(userService).loggedInUser()
            doReturn(value = todo).`when`(userService).updateTodoItem(id = id, updateTodoDto = updateTodoDto)
            // When
            val response: TodoDto = userController.updateTodo(id = "$id", updateTodoDto = updateTodoDto,
                resultOfValidation = bindingResult)
            // Then
            assertEquals("${todo.id}", response.id)
            assertEquals(todo.description, response.description)
            assertFalse(response.finished!!)
        }

        @Test
        @Throws(ServerException::class)
        fun given_whenDeleteTodo_thenReturn200() {
            // Given
            val id: UUID = UUID.randomUUID()
            doReturn(value = user).`when`(userService).loggedInUser()
            // When
            userController.deleteTodo(id = "$id")
            // Then
            verify(userService).deleteTodoItem(id = id)
        }
    }
}