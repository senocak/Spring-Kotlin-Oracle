package com.github.senocak.domain.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.github.senocak.util.validation.PasswordMatches
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@JsonPropertyOrder("name", "username", "email", "roles", "resourceUrl")
class UserResponse(
    @JsonProperty("name")
    @Schema(example = "Lorem Ipsum", description = "Name of the user", required = true, name = "name", type = "String")
    var name: String,

    @Schema(example = "lorem@ipsum.com", description = "Email of the user", required = true, name = "email", type = "String")
    var email: String,

    @ArraySchema(schema = Schema(example = "ROLE_USER", description = "Roles of the user", required = true, name = "roles"))
    var roles: List<String>,

    @Schema(example = "1253123123", description = "Email activation datetime", required = false, name = "emailActivatedAt", type = "Long")
    var emailActivatedAt: Long? = null
): BaseDto()

@PasswordMatches
data class UpdateUserDto(
    @Schema(example = "Anil", description = "Name", required = true, name = "name", type = "String")
    @field:Size(min = 4, max = 40)
    var name: String? = null,

    @Schema(example = "Anil123", description = "Password", required = true, name = "password", type = "String")
    var password: String? = null,

    @JsonProperty("password_confirmation")
    @Schema(example = "Anil123", description = "Password confirmation", required = true, name = "password", type = "String")
    var passwordConfirmation: String? = null
): BaseDto()