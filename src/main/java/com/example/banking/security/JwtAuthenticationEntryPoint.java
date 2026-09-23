package com.example.banking.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Called by Spring Security whenever a request reaches a protected endpoint
 * without being authenticated.
 *
 * This class returns the same JSON error format used by the rest of the API
 * instead of Spring Security's default 401 response.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String MESSAGE =
            "Authentication is required to access this resource";

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        SecurityResponseWriter.writeUnauthorized(
                request,
                response,
                objectMapper,
                MESSAGE
        );
    }
}