package com.github.senocak.controller

import com.github.senocak.TestConstants
import com.github.senocak.createTestUser
import com.github.senocak.domain.User
import com.github.senocak.domain.dto.UpdateUserDto
import com.github.senocak.domain.dto.UserResponse
import com.github.senocak.exception.ServerException
import com.github.senocak.service.UserService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.http.MediaType
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import com.github.senocak.domain.Role
import com.github.senocak.util.RoleName
import com.github.senocak.exception.RestExceptionHandler
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.BindingResult
import jakarta.servlet.http.HttpServletRequest
import org.mockito.InjectMocks
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.mockito.kotlin.mock

@Tag("unit")
@ExtendWith(MockitoExtension::class)
@DisplayName("Unit Tests for UserController")
class UserControllerTest {
    @InjectMocks lateinit var userController: UserController
    private val userService: UserService = mock<UserService>()
    private val passwordEncoder: PasswordEncoder = mock<PasswordEncoder>()
    private val bindingResult: BindingResult = mock<BindingResult>()
    private val user: User = createTestUser()

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
    internal inner class GetUserByTemplateTest {
        private lateinit var mockMvc: MockMvc

        @BeforeEach
        fun setup() {
            userController = UserController(userService, passwordEncoder)
            mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(RestExceptionHandler())
                .build()
        }

        @Test
        fun `given_whenGetUserByTemplate_thenReturn200`() {
            // Given
            val testUser = User(name = "Test User", email = "test@test.com", password = "password")
            val role = Role(name = RoleName.ROLE_USER)
            testUser.roles = listOf(role)

            val page = PageImpl(listOf(testUser), PageRequest.of(0, 10), 1)
            Mockito.`when`(userService.getUserByTemplate(0, 10, null, null, null, "AND"))
                .thenReturn(page)

            // When
            mockMvc.perform(
                get("${BaseController.V1_USER_URL}?page=0&size=10")
                    .header("X-API-VERSION", "template")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            // Then
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.page").value(1))  // PaginationResponse uses 1-based page numbers
                .andExpect(jsonPath("$.pages").value(1))  // Total number of pages
                .andExpect(jsonPath("$.total").value(1))  // Total number of elements
                .andExpect(jsonPath("$.items[0].name").value("Test User"))
                .andExpect(jsonPath("$.items[0].email").value("test@test.com"))
        }

        @Test
        fun `givenFilters_whenGetUserByTemplate_thenReturnFilteredResults`() {
            // Given
            val testUser = User(name = "Test User", email = "test@test.com", password = "password")
            val role = Role(name = RoleName.ROLE_USER)
            testUser.roles = listOf(role)

            val page = PageImpl(listOf(testUser), PageRequest.of(0, 10), 1)
            Mockito.`when`(userService.getUserByTemplate(0, 10, "Test", "Test", null, "AND"))
                .thenReturn(page)

            // When
            mockMvc.perform(
                get("${BaseController.V1_USER_URL}?page=0&size=10&q=Test")
                    .header("X-API-VERSION", "template")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            // Then
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.items[0].name").value("Test User"))
        }
    }

    @Nested
    internal inner class GetAllUsersTest {
        private lateinit var mockMvc: MockMvc

        @BeforeEach
        fun setup() {
            userController = UserController(userService, passwordEncoder)
            mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(RestExceptionHandler())
                .build()
        }

        @Test
        fun `given_whenGetAllUsersWithTemplate_thenReturn200`() {
            // Given
            val testUser = User(name = "Test User", email = "test@test.com", password = "password")
            val role = Role(name = RoleName.ROLE_USER)
            testUser.roles = listOf(role)

            val page = PageImpl(listOf(testUser), PageRequest.of(0, 10), 1)
            Mockito.`when`(userService.getUserByTemplate(0, 10, null, null, null, "AND"))
                .thenReturn(page)

            // When
            mockMvc.perform(
                get("${BaseController.V1_USER_URL}?page=0&size=10")
                    .header("X-API-VERSION", "template")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            // Then
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pages").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Test User"))
                .andExpect(jsonPath("$.items[0].email").value("test@test.com"))
        }

        @Test
        fun `givenFilters_whenGetAllUsersWithClient_thenReturnFilteredResults`() {
            // Given
            val testUser = User(name = "Test User", email = "test@test.com", password = "password")
            val role = Role(name = RoleName.ROLE_USER)
            testUser.roles = listOf(role)

            val page = PageImpl(listOf(testUser), PageRequest.of(0, 10), 1)
            val roleIds = listOf("role-id-1")
            Mockito.`when`(userService.getUsersWithJdbcClient(0, 10, "Test", "Test", roleIds, "OR"))
                .thenReturn(page)

            // When
            mockMvc.perform(
                get("${BaseController.V1_USER_URL}?page=0&size=10&q=Test&roleIds=role-id-1&operator=OR")
                    .header("X-API-VERSION", "client")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            // Then
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.items[0].name").value("Test User"))
                .andExpect(jsonPath("$.items[0].email").value("test@test.com"))
        }
    }

    @Nested
    internal inner class GetUserByJdbcClientTest {
        private lateinit var mockMvc: MockMvc

        @BeforeEach
        fun setup() {
            userController = UserController(userService, passwordEncoder)
            mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(RestExceptionHandler())
                .build()
        }

        @Test
        fun `given_whenGetUserByJdbcClient_thenReturn200`() {
            // Given
            val testUser = User(name = "Test User", email = "test@test.com", password = "password")
            val role = Role(name = RoleName.ROLE_USER)
            testUser.roles = listOf(role)

            val page = PageImpl(listOf(testUser), PageRequest.of(0, 10), 1)
            Mockito.`when`(userService.getUsersWithJdbcClient(0, 10, null, null, null, "AND"))
                .thenReturn(page)

            // When
            mockMvc.perform(
                get("${BaseController.V1_USER_URL}?page=0&size=10")
                    .header("X-API-VERSION", "client")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            // Then
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.page").value(1))  // PaginationResponse uses 1-based page numbers
                .andExpect(jsonPath("$.pages").value(1))  // Total number of pages
                .andExpect(jsonPath("$.total").value(1))  // Total number of elements
                .andExpect(jsonPath("$.items[0].name").value("Test User"))
                .andExpect(jsonPath("$.items[0].email").value("test@test.com"))
        }

        @Test
        fun `givenFilters_whenGetUserByJdbcClient_thenReturnFilteredResults`() {
            // Given
            val testUser = User(name = "Test User", email = "test@test.com", password = "password")
            val role = Role(name = RoleName.ROLE_USER)
            testUser.roles = listOf(role)

            val page = PageImpl(listOf(testUser), PageRequest.of(0, 10), 1)
            Mockito.`when`(userService.getUsersWithJdbcClient(0, 10, "Test", "Test", null, "AND"))
                .thenReturn(page)

            // When
            mockMvc.perform(
                get("${BaseController.V1_USER_URL}?page=0&size=10&q=Test")
                    .header("X-API-VERSION", "client")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            // Then
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.items[0].name").value("Test User"))
        }
    }
}
