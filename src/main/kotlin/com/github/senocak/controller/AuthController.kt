package com.github.senocak.controller

import com.github.senocak.domain.User
import com.github.senocak.domain.dto.LoginRequest
import com.github.senocak.domain.dto.RegisterRequest
import com.github.senocak.domain.dto.UserResponse
import com.github.senocak.domain.dto.UserWrapperResponse
import com.github.senocak.exception.ServerException
import com.github.senocak.security.JwtTokenProvider
import com.github.senocak.service.RoleService
import com.github.senocak.service.UserService
import com.github.senocak.util.OmaErrorMessageType
import com.github.senocak.util.RoleName
import com.github.senocak.util.convertEntityToDto
import com.github.senocak.util.logger
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping(BaseController.V1_AUTH_URL)
@Tag(name = "Authentication", description = "AUTH API")
class AuthController(
    private val userService: UserService,
    private val tokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val roleService: RoleService,
    @Value("\${app.jwtExpirationInMs}") private val jwtExpirationInMs: Long,
): BaseController() {
    private val log: Logger by logger()

    @PostMapping("/login")
    @Operation(summary = "Login Endpoint", tags = ["Authentication"])
    @ApiResponse(responseCode = "201", description = "successful operation", content = [
        Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = UserWrapperResponse::class))])
    @Throws(ServerException::class)
    fun login(
        @Parameter(description = "Request body to login", required = true) @Validated @RequestBody loginRequest: LoginRequest,
        resultOfValidation: BindingResult
    ): ResponseEntity<UserWrapperResponse> =
        validate(resultOfValidation = resultOfValidation)
            .run { authenticationManager.authenticate(UsernamePasswordAuthenticationToken(loginRequest.email, loginRequest.password)) }
            .run { userService.findByEmail(email = loginRequest.email) }
            .run {
                val generateUserWrapperResponse: UserWrapperResponse = generateUserWrapperResponse(user = this)
                val httpHeaders: HttpHeaders = userIdHeader(userId = "${this.id}")
                    .apply { this.add("jwtExpiresIn", "$jwtExpirationInMs") }
                ResponseEntity.status(HttpStatus.OK).headers(httpHeaders).body(generateUserWrapperResponse)
            }

    @PostMapping("/register")
    @ApiResponse(responseCode = "200", description = "successful operation", content = [
        Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = Map::class))]
    )
    @ResponseStatus(code = HttpStatus.CREATED)
    @Throws(ServerException::class)
    fun register(
        @Parameter(description = "Request body to register", required = true) @Validated @RequestBody signUpRequest: RegisterRequest,
        resultOfValidation: BindingResult
    ): Map<String, String> {
        validate(resultOfValidation = resultOfValidation)
        if (userService.existsByEmail(email = signUpRequest.email))
            "unique_email".plus(other = ": ${signUpRequest.email}")
                .apply { log.error(this) }
                .run { throw ServerException(omaErrorMessageType = OmaErrorMessageType.JSON_SCHEMA_VALIDATOR, variables = arrayOf(this)) }
        val user: User = User(name = signUpRequest.name, email = signUpRequest.email, password = passwordEncoder.encode(signUpRequest.password))
            .also { it.roles = listOf(element = roleService.findByName(roleName = RoleName.fromString(r = RoleName.ROLE_USER.role))) }
        val result: User = userService.save(user = user)
            .also { log.info("UserRegisteredEvent is published for user: $user") }
        log.info("User created. User: $result")
        return mapOf("message" to "email_has_to_be_verified")
    }

    /**
     * Generate UserWrapperResponse with given UserResponse
     * @param user -- User entity that contains user data
     * @return UserWrapperResponse
     */
    private fun generateUserWrapperResponse(user: User): UserWrapperResponse {
        val userResponse: UserResponse = user.convertEntityToDto()
        val jwtToken: String = tokenProvider.generateJwtToken(email = user.email!!, roles = userResponse.roles)
        return UserWrapperResponse(userResponse = userResponse, token = jwtToken)
            .also { log.info("UserWrapperResponse is generated. UserWrapperResponse: $it") }
    }

    @GetMapping("/csrf")
    fun csrf(csrfToken: CsrfToken): CsrfToken =
        csrfToken
}