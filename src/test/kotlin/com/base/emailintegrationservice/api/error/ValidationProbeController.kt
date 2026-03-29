package com.base.emailintegrationservice.api.error

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class ProbeRequest(
    @field:NotBlank val title: String,
)

/**
 * Minimal endpoint used only in WebMvc tests to assert validation → ApiErrorResponse mapping.
 */
@RestController
@RequestMapping("/__probe")
class ValidationProbeController {
    @PostMapping
    fun post(
        @Valid @RequestBody body: ProbeRequest,
    ) = Unit
}
