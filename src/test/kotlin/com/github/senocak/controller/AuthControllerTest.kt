package com.github.senocak.controller

import com.github.senocak.TestConstants.USER_EMAIL
import com.github.senocak.TestConstants.USER_NAME
import com.github.senocak.TestConstants.USER_PASSWORD
import com.github.senocak.createTestUser
import com.github.senocak.domain.User
import com.github.senocak.domain.dto.LoginRequest
import com.github.senocak.domain.dto.RegisterRequest
import com.github.senocak.domain.dto.UserWrapperResponse
import com.github.senocak.exception.ServerException
import com.github.senocak.security.JwtTokenProvider
import com.github.senocak.service.UserService
import com.github.senocak.util.OmaErrorMessageType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import org.mockito.ArgumentMatchers.anyList
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.BindingResult

@Tag("unit")
@ExtendWith(MockitoExtension::class)
@DisplayName("Unit Tests for AuthController")
class AuthControllerTest {
    lateinit var authController: AuthController
    private val userService: UserService = mock<UserService>()
    private val tokenProvider: JwtTokenProvider = mock<JwtTokenProvider>()
    private val passwordEncoder: PasswordEncoder = mock<PasswordEncoder>()
    private val authenticationManager: AuthenticationManager = mock<AuthenticationManager>()
    private val authentication: Authentication = mock<Authentication>()
    private val bindingResult: BindingResult = mock<BindingResult>()
    private var user: User = createTestUser()

    @BeforeEach
    fun init() {
        authController = AuthController(
            userService = userService,
            tokenProvider = tokenProvider,
            passwordEncoder = passwordEncoder,
            authenticationManager = authenticationManager,
            jwtExpirationInMs = 100,
        )
    }

    @Nested
    internal inner class LoginTest {
        private val loginRequest: LoginRequest = LoginRequest(email = USER_EMAIL, password = USER_PASSWORD)
        @BeforeEach
        fun setup() {
            loginRequest.email = USER_NAME
            loginRequest.password = USER_PASSWORD
        }

        @Test
        @Throws(ServerException::class)
        fun givenNotActivatedUser_whenLogin_thenThrowException() {
            // Given
            user.emailActivatedAt = null
            whenever(methodCall = authenticationManager.authenticate(UsernamePasswordAuthenticationToken(loginRequest.email, loginRequest.password)))
                .thenReturn(authentication)
            whenever(methodCall = userService.findByEmail(email = loginRequest.email)).thenReturn(user)
            val generatedToken = "generatedToken"
            whenever(methodCall = tokenProvider.generateJwtToken(email = eq(user.email!!), roles = anyList())).thenReturn(generatedToken)
            // When
            val response = Executable {
                authController.login(loginRequest = loginRequest, resultOfValidation = bindingResult)
            }
            // Then
            val assertThrows = assertThrows(ServerException::class.java, response)
            assertEquals(HttpStatus.UNAUTHORIZED, assertThrows.statusCode)
            assertEquals(OmaErrorMessageType.UNAUTHORIZED, assertThrows.omaErrorMessageType)
            assertEquals(1, assertThrows.variables.size)
            assertEquals("email_not_activated", assertThrows.variables.first())
        }

        @Test
        @Throws(ServerException::class)
        fun givenSuccessfulPath_whenLogin_thenReturn200() {
            // Given
            whenever(methodCall = authenticationManager.authenticate(UsernamePasswordAuthenticationToken(loginRequest.email, loginRequest.password)))
                .thenReturn(authentication)
            whenever(methodCall = userService.findByEmail(email = loginRequest.email)).thenReturn(user)
            val generatedToken = "generatedToken"
            whenever(methodCall = tokenProvider.generateJwtToken(email = eq(user.email!!), roles = anyList())).thenReturn(generatedToken)
            // When
            val response: ResponseEntity<UserWrapperResponse> = authController.login(loginRequest = loginRequest, resultOfValidation = bindingResult)
            // Then
            assertNotNull(response)
            assertNotNull(response.body)
            assertEquals(generatedToken, response.body!!.token)
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(user.name, response.body!!.userResponse.name)
            assertEquals(user.email, response.body!!.userResponse.email)
            assertEquals(user.roles.size, response.body!!.userResponse.roles.size)
            assertEquals("generatedToken", response.body!!.token)
        }
    }

    @Nested
    internal inner class RegisterTest {
        private val registerRequest: RegisterRequest = RegisterRequest(
            name = USER_NAME,
            email = USER_EMAIL,
            password = USER_PASSWORD
        )

        @Test
        fun givenExistMail_whenRegister_thenThrowServerException() {
            // Given
            whenever(methodCall = userService.existsByEmail(email = registerRequest.email)).thenReturn(true)
            // When
            val closureToTest = Executable { authController.register(signUpRequest = registerRequest, resultOfValidation = bindingResult) }
            // Then
            assertThrows(ServerException::class.java, closureToTest)
        }

        @Test
        fun given_whenRegister_thenAssertResult() {
            // Given
            doReturn(value = "pass1").`when`(passwordEncoder).encode(registerRequest.password)
            doReturn(value = user).`when`(userService).save(user = any())
            // When
            val response: Map<String, String> = authController.register(signUpRequest = registerRequest, resultOfValidation = bindingResult)
            // Then
            assertNotNull(response)
            assertNotNull(response["message"])
            assertEquals("email_has_to_be_verified", response["message"])
        }
    }
}
