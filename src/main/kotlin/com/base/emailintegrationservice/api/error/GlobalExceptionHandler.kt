package com.base.emailintegrationservice.api.error

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ApiErrorResponse> {
        val statusCode = ex.statusCode
        val resolved = HttpStatus.resolve(statusCode.value())
        val body =
            ApiErrorResponse(
                status = statusCode.value(),
                code = resolved?.name ?: "HTTP_${statusCode.value()}",
                message = ex.reason ?: resolved?.reasonPhrase ?: "Error",
            )
        return ResponseEntity.status(statusCode).body(body)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiErrorResponse> {
        val body =
            ApiErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                code = "BAD_REQUEST",
                message = ex.message ?: "Invalid request",
            )
        return ResponseEntity.badRequest().body(body)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val violations =
            ex.bindingResult.fieldErrors.map { err ->
                FieldViolation(
                    field = err.field,
                    message = err.defaultMessage ?: "Invalid value",
                )
            }
        val body =
            ApiErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                code = "VALIDATION_ERROR",
                message = "Validation failed",
                errors = violations,
            )
        return ResponseEntity.badRequest().body(body)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ApiErrorResponse> {
        val violations =
            ex.constraintViolations.map { v ->
                val path = v.propertyPath.toString()
                FieldViolation(
                    field = path.substringAfterLast('.').ifEmpty { path },
                    message = v.message,
                )
            }
        val body =
            ApiErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                code = "VALIDATION_ERROR",
                message = "Validation failed",
                errors = violations,
            )
        return ResponseEntity.badRequest().body(body)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponse> {
        val body =
            ApiErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                code = "MALFORMED_JSON",
                message = "Request body is not valid JSON",
            )
        return ResponseEntity.badRequest().body(body)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(ex: MissingServletRequestParameterException): ResponseEntity<ApiErrorResponse> {
        val body =
            ApiErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                code = "MISSING_PARAMETER",
                message = "Required query parameter '${ex.parameterName}' is missing",
            )
        return ResponseEntity.badRequest().body(body)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ApiErrorResponse> {
        val name = ex.name ?: "parameter"
        val body =
            ApiErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                code = "TYPE_MISMATCH",
                message = "Parameter '$name' has an invalid value",
            )
        return ResponseEntity.badRequest().body(body)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ApiErrorResponse> {
        log.error("Unhandled error", ex)
        val body =
            ApiErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                code = "INTERNAL_ERROR",
                message = "An unexpected error occurred",
            )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body)
    }
}
