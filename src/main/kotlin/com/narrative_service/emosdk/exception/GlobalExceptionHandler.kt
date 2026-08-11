package com.narrative_service.emosdk.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(
        NarrativeNotFoundException::class,
        NarrativeNodeNotFoundException::class,
        NarrativeLayerNotFoundException::class,
        ProjectNotFoundException::class
    )
    fun handleNotFound(
        ex: RuntimeException
    ): ResponseEntity<Map<String, String>> {

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                mapOf(
                    "error" to "NOT_FOUND",
                    "message" to ex.message.orEmpty()
                )
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(
        ex: IllegalArgumentException
    ): ResponseEntity<Map<String, String>> {

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                mapOf(
                    "error" to "BAD_REQUEST",
                    "message" to ex.message.orEmpty()
                )
            )
    }
}
