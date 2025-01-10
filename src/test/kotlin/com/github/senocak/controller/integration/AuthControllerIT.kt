package com.github.senocak.controller.integration

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.senocak.TestConstants.USER_EMAIL
import com.github.senocak.TestConstants.USER_NAME
import com.github.senocak.TestConstants.USER_PASSWORD
import com.github.senocak.config.SpringBootTestConfig
import com.github.senocak.controller.AuthController
import com.github.senocak.controller.BaseController
import com.github.senocak.domain.User
import com.github.senocak.domain.dto.LoginRequest
import com.github.senocak.domain.dto.RegisterRequest
import com.github.senocak.exception.RestExceptionHandler
import com.github.senocak.service.UserService
import com.github.senocak.util.OmaErrorMessageType
import com.github.senocak.util.RoleName
import java.util.Date
import java.util.UUID
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasSize
import org.hamcrest.core.IsEqual.equalTo
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/**
 * This integration test class is written for
 * @see AuthController
 */
@SpringBootTestConfig
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Integration Tests for AuthController")
class AuthControllerIT {
    @Autowired private lateinit var authController: AuthController
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Autowired private lateinit var userService: UserService
    @Autowired private lateinit var passwordEncoder: PasswordEncoder
    @Autowired private lateinit var restExceptionHandler: RestExceptionHandler

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun beforeEach() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
            .setControllerAdvice(restExceptionHandler)
            .build()
    }

    @AfterAll
    fun afterAll() {
        userService.deleteAllUsers()
    }

    @BeforeAll
    fun beforeAll() {
        User(name = "anil1", email = "anil1@senocak.com", password = passwordEncoder.encode("asenocak"))
            .also {
                it.id = UUID.fromString("2cb9374e-4e52-4142-a1af-16144ef4a27d")
                it.roles = listOf(RoleName.ROLE_USER.role, RoleName.ROLE_ADMIN.role)
                it.emailActivatedAt = Date()
            }
            .run {
                userService.save(user = this)
            }

        User(name = "anil2", email = "anil2@gmail.com", password = passwordEncoder.encode("asenocak"))
            .also {
                it.id = UUID.fromString("3cb9374e-4e52-4142-a1af-16144ef4a27d")
                it.roles = listOf(RoleName.ROLE_USER.role)
                it.emailActivatedAt = Date()
            }
            .run {
                userService.save(user = this)
            }

        User(name = "anil3", email = "anil3@gmail.com", password = passwordEncoder.encode("asenocak"))
            .also {
                it.id = UUID.fromString("4cb9374e-4e52-4142-a1af-16144ef4a27d")
                it.roles = listOf(RoleName.ROLE_USER.role)
            }
            .run {
                userService.save(user = this)
            }
    }

    @Nested
    @Order(1)
    @DisplayName("Test class for login scenarios")
    @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
    internal inner class LoginTest {
        private val request: LoginRequest = LoginRequest(email = "", password = "")

        @Test
        @Order(1)
        @DisplayName("ServerException is expected since request body is not valid")
        @Throws(Exception::class)
        fun givenInvalidSchema_whenLogin_thenThrowServerException() {
            // Given
            val requestBuilder: RequestBuilder = MockMvcRequestBuilders
                .post("${BaseController.V1_AUTH_URL}/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeValueAsString(request))
            // When
            val perform: ResultActions = mockMvc.perform(requestBuilder)
            // Then
            perform
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.statusCode",
                    equalTo(HttpStatus.BAD_REQUEST.value())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.error.id",
                    equalTo(OmaErrorMessageType.JSON_SCHEMA_VALIDATOR.messageId)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.error.text",
                    equalTo(OmaErrorMessageType.JSON_SCHEMA_VALIDATOR.text)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.variables",
                    hasSize<Any>(4)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.variables",
                    containsInAnyOrder("password: {not_blank}", "password: {min_max_length}",
                        "email: {min_max_length}", "email: {not_blank}")))
        }

        @Test
        @Order(2)
        @DisplayName("ServerException is expected since credentials are not valid")
        @Throws(Exception::class)
        fun givenInvalidCredentials_whenLogin_thenThrowServerException() {
            // Given
            request.email = "anil1@senocak.com"
            request.password = "not_asenocak"
            val requestBuilder: RequestBuilder = MockMvcRequestBuilders
                .post("${BaseController.V1_AUTH_URL}/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeValueAsString(request))
            // When
            val perform: ResultActions = mockMvc.perform(requestBuilder)
            // Then
            perform
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.statusCode",
                    equalTo(HttpStatus.UNAUTHORIZED.value())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.error.id",
                    equalTo(OmaErrorMessageType.UNAUTHORIZED.messageId)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.error.text",
                    equalTo(OmaErrorMessageType.UNAUTHORIZED.text)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.variables",
                    hasSize<Any>(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.variables[0]",
                    equalTo("Username or password invalid. AuthenticationCredentialsNotFoundException occurred for anil1")))
        }

        @Test
        @Order(3)
        @DisplayName("ServerException is expected since user not existed")
        @Throws(Exception::class)
        fun givenNotActivatedUser_whenLogin_thenThrowServerException() {
            // Given
            request.email = "not@exist.com"
            request.password = "asenocak"
            val requestBuilder: RequestBuilder = MockMvcRequestBuilders
                .post("${BaseController.V1_AUTH_URL}/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeValueAsString(request))
            // When
            val perform: ResultActions = mockMvc.perform(requestBuilder)
            // Then
            perform
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.statusCode",
                    equalTo(HttpStatus.NOT_FOUND.value())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.error.id",
                    equalTo(OmaErrorMessageType.NOT_FOUND.messageId)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.error.text",
                    equalTo(OmaErrorMessageType.NOT_FOUND.text)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.variables",
                    hasSize<Any>(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.variables[0]",
                    equalTo("user_not_found")))
        }

        @Test
        @Order(4)
        @DisplayName("Happy path")
        @Throws(Exception::class)
        fun given_whenLogin_thenReturn200() {
            // Given
            request.email = "anil1@senocak.com"
            request.password = "asenocak"
            // When
            val perform: ResultActions = MockMvcRequestBuilders
                .post("${BaseController.V1_AUTH_URL}/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeValueAsString(request))
                .run { mockMvc.perform(this) }
            // Then
            perform
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.user.email", equalTo(request.email)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.user.roles", hasSize<Any>(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.token", IsNull.notNullValue()))
        }
    }

    @Nested
    @Order(2)
    @DisplayName("Test class for register scenarios")
    @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
    internal inner class RegisterTest {
        private val registerRequest: RegisterRequest = RegisterRequest(name = "", email = "", password = "")

        @Test
        @Order(1)
        @DisplayName("ServerException is expected since request body is not valid")
        @Throws(Exception::class)
        fun givenInvalidSchema_whenRegister_thenThrowServerException() {
            // Given
            val requestBuilder: RequestBuilder = MockMvcRequestBuilders
                .post("${BaseController.V1_AUTH_URL}/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeValueAsString(registerRequest))
            // When
            val perform: ResultActions = mockMvc.perform(requestBuilder)
            // Then
            perform
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.statusCode",
                    equalTo(HttpStatus.BAD_REQUEST.value())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.error.id",
                    equalTo(OmaErrorMessageType.JSON_SCHEMA_VALIDATOR.messageId)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.error.text",
                    equalTo(OmaErrorMessageType.JSON_SCHEMA_VALIDATOR.text)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.variables",
                    hasSize<Any>(5)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.variables",
                    containsInAnyOrder(
                        "password: {min_max_length}",
                        "name: {not_blank}",
                        "name: {min_max_length}",
                        "email: Invalid email",
                        "password: {not_blank}"
                    )))
        }

        @Test
        @Order(2)
        @DisplayName("ServerException is expected since there is already user with username")
        @Throws(Exception::class)
        fun givenEmailExist_whenRegister_thenThrowServerException() {
            // Given
            registerRequest.name = USER_NAME
            registerRequest.email = USER_EMAIL
            registerRequest.password = USER_PASSWORD
            val requestBuilder: RequestBuilder = MockMvcRequestBuilders
                .post("${BaseController.V1_AUTH_URL}/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeValueAsString(registerRequest))
            // When
            val perform: ResultActions = mockMvc.perform(requestBuilder)
            // Then
            perform
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.statusCode",
                    equalTo(HttpStatus.BAD_REQUEST.value())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.error.id",
                    equalTo(OmaErrorMessageType.JSON_SCHEMA_VALIDATOR.messageId)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.error.text",
                    equalTo(OmaErrorMessageType.JSON_SCHEMA_VALIDATOR.text)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.variables",
                    hasSize<Any>(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.exception.variables[0]",
                    equalTo("unique_email: $USER_EMAIL")))
        }

        @Test
        @Order(3)
        @DisplayName("Happy path")
        fun given_whenRegister_thenReturn201() {
            // Given
            registerRequest.name = USER_NAME
            registerRequest.email = "userNew@email.com"
            registerRequest.password = USER_PASSWORD
            // When
            val perform: ResultActions =  MockMvcRequestBuilders
                .post("${BaseController.V1_AUTH_URL}/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeValueAsString(registerRequest))
                .run { mockMvc.perform(this) }
            // Then
            perform
                .andExpect(MockMvcResultMatchers.status().isCreated)
                .andExpect(MockMvcResultMatchers.jsonPath("$.message", IsNull.notNullValue()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message",
                    equalTo("email_has_to_be_verified")))
        }
    }

    /**
     * @param value -- an object that want to be serialized
     * @return -- string
     * @throws JsonProcessingException -- throws JsonProcessingException
     */
    @Throws(JsonProcessingException::class)
    private fun writeValueAsString(value: Any): String = objectMapper.writeValueAsString(value)
}

