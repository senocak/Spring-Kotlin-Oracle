package com.github.senocak.util

import com.fasterxml.jackson.annotation.JsonValue

enum class RoleName(@JsonValue val role: String) {
    ROLE_USER(role = AppConstants.USER),
    ROLE_ADMIN(role = AppConstants.ADMIN);

    companion object {
        fun fromString(r: String?): RoleName {
            for (it: RoleName in entries) {
                if (it.role == r || it.name == r) {
                    return it
                }
            }
            //return null
            throw IllegalArgumentException("Invalid role: $r")
        }
    }
}

enum class OmaErrorMessageType(val messageId: String, val text: String) {
    BASIC_INVALID_INPUT("SVC0001", "Invalid input value for message part %1"),
    GENERIC_SERVICE_ERROR("SVC0002", "The following service error occurred: %1. Error code is %2"),
    DETAILED_INVALID_INPUT("SVC0003", "Invalid input value for %1 %2: %3"),
    EXTRA_INPUT_NOT_ALLOWED("SVC0004", "Input %1 %2 not permitted in request"),
    MANDATORY_INPUT_MISSING("SVC0005", "Mandatory input %1 %2 is missing from request"),
    UNAUTHORIZED("SVC0006", "UnAuthorized Endpoint"),
    JSON_SCHEMA_VALIDATOR("SVC0007", "Schema failed."),
    NOT_FOUND("SVC0008", "Entry is not found")
}
