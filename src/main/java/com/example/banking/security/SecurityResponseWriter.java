package com.example.banking.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.example.banking.dto.ErrorResponse;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes a 401 response in the SAME JSON shape (ErrorResponse) that GlobalExceptionHandler
 * uses for every other error in this API.
 *
 * WHY this class exists: an exception thrown inside a servlet Filter runs BEFORE Spring MVC's
 * exception-handling machinery, so @RestControllerAdvice (GlobalExceptionHandler) never sees
 * it - a filter has to write the HTTP response itself, by hand, instead of just throwing.
 */
final class SecurityResponseWriter {

    private SecurityResponseWriter() {
    }

    static void writeUnauthorized(HttpServletRequest request, HttpServletResponse response,
                                  ObjectMapper objectMapper, String message) throws IOException {
        ErrorResponse body = new ErrorResponse(Instant.now(), HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), message, request.getRequestURI(), Map.of());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}