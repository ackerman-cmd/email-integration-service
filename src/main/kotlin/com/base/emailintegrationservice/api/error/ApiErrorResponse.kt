package com.base.emailintegrationservice.api.error

data class ApiErrorResponse(
    val status: Int,
    val code: String,
    val message: String,
    val errors: List<FieldViolation>? = null,
)

data class FieldViolation(
    val field: String,
    val message: String,
)
