package com.github.senocak.controller

import com.github.senocak.domain.User
import com.github.senocak.domain.dto.ExceptionDto
import com.github.senocak.domain.dto.UpdateUserDto
import com.github.senocak.domain.dto.UserPaginationDTO
import com.github.senocak.domain.dto.UserResponse
import com.github.senocak.domain.dto.UserWrapperResponse
import com.github.senocak.exception.ServerException
import com.github.senocak.security.Authorize
import com.github.senocak.service.UserService
import com.github.senocak.util.AppConstants.ADMIN
import com.github.senocak.util.AppConstants.DEFAULT_PAGE_NUMBER
import com.github.senocak.util.AppConstants.DEFAULT_PAGE_SIZE
import com.github.senocak.util.AppConstants.SECURITY_SCHEME_NAME
import com.github.senocak.util.AppConstants.USER
import com.github.senocak.util.OmaErrorMessageType
import com.github.senocak.util.convertEntityToDto
import com.github.senocak.util.logger
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Validated
@RestController
@Authorize
@RequestMapping(BaseController.V1_USER_URL)
@Tag(name = "User", description = "User Controller")
class UserController(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
): BaseController() {
    private val log: Logger by logger()

    @Throws(ServerException::class)
    @Operation(summary = "Get me", tags = ["User"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = UserWrapperResponse::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ExceptionDto::class))))
        ],
        security = [SecurityRequirement(name = SECURITY_SCHEME_NAME, scopes = [ADMIN, USER])]
    )
    @GetMapping("/me")
    fun me(): UserResponse =
        userService.loggedInUser().convertEntityToDto()

    @PatchMapping("/me")
    @Operation(summary = "Update user by username", tags = ["User"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = HashMap::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ExceptionDto::class))))
        ],
        security = [SecurityRequirement(name = SECURITY_SCHEME_NAME, scopes = [ADMIN, USER])]
    )
    @Throws(ServerException::class)
    fun patchMe(request: HttpServletRequest,
        @Parameter(description = "Request body to update", required = true) @Validated @RequestBody userDto: UpdateUserDto,
        resultOfValidation: BindingResult
    ): UserResponse {
        validate(resultOfValidation = resultOfValidation)
        val user: User = userService.loggedInUser()
        val name: String? = userDto.name
        if (!name.isNullOrEmpty())
            user.name = name
        val password: String? = userDto.password
        val passwordConfirmation: String? = userDto.passwordConfirmation
        if (!password.isNullOrEmpty()) {
            if (passwordConfirmation.isNullOrEmpty()) {
                "password_confirmation_not_provided"
                    .apply { log.error(this) }
                    .run { throw ServerException(omaErrorMessageType = OmaErrorMessageType.BASIC_INVALID_INPUT,
                        variables = arrayOf(this), statusCode = HttpStatus.BAD_REQUEST) }
            }
            if (passwordConfirmation != password) {
                "password_and_confirmation_not_matched"
                    .apply { log.error(this) }
                    .run { throw ServerException(omaErrorMessageType = OmaErrorMessageType.BASIC_INVALID_INPUT,
                        variables = arrayOf(this), statusCode = HttpStatus.BAD_REQUEST) }
            }
            user.password = passwordEncoder.encode(password)
        }
        return userService.save(user = user)
            .run user@ {
                this@user.convertEntityToDto()
            }
    }

    @Throws(ServerException::class)
    @Authorize(roles = [ADMIN])
    @GetMapping(headers = ["X-API-VERSION=template"])
    @Operation(
        summary = "All Users",
        tags = ["User"],
        responses = [
            ApiResponse(responseCode = "200", description = MediaType.APPLICATION_JSON_VALUE,
                content = arrayOf(Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = UserPaginationDTO::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ExceptionDto::class))))
        ],
        security = [SecurityRequirement(name = SECURITY_SCHEME_NAME, scopes = [ADMIN])]
    )
    fun getUserByTemplate(
        @Parameter(name = "page", description = "Page number", example = DEFAULT_PAGE_NUMBER) @RequestParam(defaultValue = "0", required = false) page: Int,
        @Parameter(name = "size", description = "Page size", example = DEFAULT_PAGE_SIZE) @RequestParam(defaultValue = "\${spring.data.web.pageable.default-page-size:10}", required = false) size: Int,
        @Parameter(name = "q", description = "Search keyword", example = "lorem") @RequestParam(required = false) q: String?,
        @Parameter(name = "roleIds", description = "List of role ids", example = "12b9374e-4e52-4142-a1af-16144ef4a27d") @RequestParam(required = false) roleIds: List<String>?,
        @Parameter(name = "operator", description = "Date range end", example = "AND") @RequestParam(required = false, defaultValue = "AND") operator: String? = "AND",
    ): UserPaginationDTO = run {
        val usersPage: Page<User> = userService.getUserByTemplate(page = page, size = size, name = q,
            email = q, roleIds = roleIds, operator = operator)
        UserPaginationDTO(
            pageModel = usersPage,
            items = usersPage.content.map { user: User -> user.convertEntityToDto() },
            sortBy = "createdAt",
            sort = "desc"
        )
    }

    @Throws(ServerException::class)
    @Authorize(roles = [ADMIN])
    @GetMapping(headers = ["X-API-VERSION=client"])
    @Operation(
        summary = "All Users using JdbcClient",
        tags = ["User"],
        responses = [
            ApiResponse(responseCode = "200", description = MediaType.APPLICATION_JSON_VALUE,
                content = arrayOf(Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = UserPaginationDTO::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ExceptionDto::class))))
        ],
        security = [SecurityRequirement(name = SECURITY_SCHEME_NAME, scopes = [ADMIN])]
    )
    fun getUserByJdbcClient(
        @Parameter(name = "page", description = "Page number", example = DEFAULT_PAGE_NUMBER) @RequestParam(defaultValue = "0", required = false) page: Int,
        @Parameter(name = "size", description = "Page size", example = DEFAULT_PAGE_SIZE) @RequestParam(defaultValue = "\${spring.data.web.pageable.default-page-size:10}", required = false) size: Int,
        @Parameter(name = "q", description = "Search keyword", example = "lorem") @RequestParam(required = false) q: String?,
        @Parameter(name = "roleIds", description = "List of role ids", example = "12b9374e-4e52-4142-a1af-16144ef4a27d") @RequestParam(required = false) roleIds: List<String>?,
        @Parameter(name = "operator", description = "Logical operator for conditions (AND/OR)", example = "AND") @RequestParam(required = false, defaultValue = "AND") operator: String? = "AND",
    ): UserPaginationDTO = run {
        val usersPage: Page<User> = userService.getUsersWithJdbcClient(page = page, size = size, name = q,
            email = q, roleIds = roleIds, operator = operator)
        UserPaginationDTO(
            pageModel = usersPage,
            items = usersPage.content.map { user: User -> user.convertEntityToDto() },
            sortBy = "createdAt",
            sort = "desc"
        )
    }
}
