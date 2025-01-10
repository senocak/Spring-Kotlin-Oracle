package com.github.senocak.domain.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.github.senocak.util.validation.ValidEmail
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@JsonPropertyOrder("user", "token")
class UserWrapperResponse(
    @JsonProperty("user")
    @Schema(required = true)
    var userResponse: UserResponse,

    @Schema(example = "eyJraWQiOiJ...", description = "Jwt Token", required = false, name = "token", type = "String")
    var token: String,
): BaseDto()

data class LoginRequest(
    @JsonProperty("email")
    @Schema(example = "asenocak", description = "Email of the user", required = true, name = "email", type = "String")
    @field:NotBlank(message = "{not_blank}")
    @field:Size(min = 3, max = 50, message = "{min_max_length}")
    var email: String,

    @Schema(description = "Password of the user", name = "password", type = "String", example = "password", required = true)
    @field:NotBlank(message = "{not_blank}")
    @field:Size(min = 6, max = 20, message = "{min_max_length}")
    var password: String
): BaseDto()

data class RegisterRequest(
    @Schema(example = "Lorem Ipsum", description = "Name of the user", required = true, name = "name", type = "String")
    @field:NotBlank(message = "{not_blank}")
    @field:Size(min = 4, max = 40, message = "{min_max_length}")
    var name: String,

    @Schema(example = "lorem@ipsum.com", description = "Email of the user", required = true, name = "email", type = "String")
    @field:ValidEmail
    var email: String,

    @Schema(example = "asenocak123", description = "Password of the user", required = true, name = "password", type = "String")
    @field:NotBlank(message = "{not_blank}")
    @field:Size(min = 6, max = 20, message = "{min_max_length}")
    var password: String
): BaseDto()

