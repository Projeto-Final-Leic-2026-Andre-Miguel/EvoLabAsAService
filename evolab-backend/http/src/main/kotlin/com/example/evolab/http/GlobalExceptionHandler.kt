package com.example.evolab.http

import com.example.evolab.http.model.problem.Problem
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(exception: Exception): ResponseEntity<Any> {
        logger.error("Unhandled exception while processing request", exception)
        return Problem.UnknownError.response(HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
