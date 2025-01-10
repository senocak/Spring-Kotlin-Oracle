package com.github.senocak.exception

import com.github.senocak.domain.dto.ExceptionDto
import com.github.senocak.util.OmaErrorMessageType
import com.github.senocak.util.logger
import java.util.function.Consumer
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class RestExceptionHandler{
    private val log: Logger by logger()

    @ExceptionHandler(
        AccessDeniedException::class,
        AuthenticationCredentialsNotFoundException::class,
        com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException::class
    )
    fun handleUnAuthorized(ex: Exception): ResponseEntity<Any> =
        generateResponseEntity(httpStatus = HttpStatus.UNAUTHORIZED, variables = arrayOf(ex.message),
            omaErrorMessageType = OmaErrorMessageType.UNAUTHORIZED)

    @ExceptionHandler(
        NoHandlerFoundException::class,
        UsernameNotFoundException::class,
        NoResourceFoundException::class
    )
    fun handleNoHandlerFoundException(ex: Exception): ResponseEntity<Any> =
        generateResponseEntity(httpStatus = HttpStatus.NOT_FOUND,
            omaErrorMessageType = OmaErrorMessageType.NOT_FOUND, variables = arrayOf(ex.message))

    @ExceptionHandler(BindException::class)
    fun handleBindException(ex: BindException): ResponseEntity<Any> =
        arrayListOf("validation_error")
            .apply {
                ex.bindingResult.allErrors.forEach(Consumer { error: ObjectError ->
                    this.add(element = "${(error as FieldError).field}: ${error.defaultMessage}")
                })
            }
            .run {
                generateResponseEntity(httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
                    variables = this.toTypedArray(), omaErrorMessageType = OmaErrorMessageType.GENERIC_SERVICE_ERROR)
            }

    @ExceptionHandler(ServerException::class)
    fun handleServerException(ex: ServerException): ResponseEntity<Any> =
        generateResponseEntity(httpStatus = ex.statusCode, omaErrorMessageType = ex.omaErrorMessageType, variables = ex.variables)

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<Any> =
        generateResponseEntity(httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            variables = arrayOf("server_error", ex.message),
            omaErrorMessageType = OmaErrorMessageType.GENERIC_SERVICE_ERROR)

    /**
     * @param httpStatus -- returned code
     * @return -- returned body
     */
    private fun generateResponseEntity(
        httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
        omaErrorMessageType: OmaErrorMessageType,
        variables: Array<String?>
    ): ResponseEntity<Any> =
        log.error("Exception is handled. HttpStatus: $httpStatus, OmaErrorMessageType: $omaErrorMessageType, variables: ${variables.toList()}")
            .run { ExceptionDto() }
            .apply {
                this.statusCode = httpStatus.value()
                this.error = ExceptionDto.OmaErrorMessageTypeDto(id = omaErrorMessageType.messageId, text = omaErrorMessageType.text)
                this.variables = variables
            }
            .run { ResponseEntity.status(httpStatus).body(this) }
}
