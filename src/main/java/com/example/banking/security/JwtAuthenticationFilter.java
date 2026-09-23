package com.example.banking.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.banking.exception.InvalidTokenException;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs once per request, before Spring Security decides permit/deny.
 *
 * WHAT: reads "Authorization: Bearer <token>", validates it with the existing JwtService,
 * and if valid, tells Spring Security "this request is authenticated as this userId" by
 * putting an Authentication into the SecurityContext.
 *
 * WHY here and not in a controller: SecurityConfig's authorizeHttpRequests() decides
 * permit/deny BEFORE any controller runs, so authentication must be established earlier
 * in the filter chain - that is exactly what a Filter is for.
 *
 * Three outcomes per request:
 *   1) no Authorization header at all -> do nothing and continue the chain unauthenticated.
 *      SecurityConfig then either allows it through (public endpoint) or rejects it with 401
 *      via JwtAuthenticationEntryPoint (protected endpoint).
 *   2) a "Bearer" token that IS valid -> set the SecurityContext, continue the chain.
 *   3) a "Bearer" token that is NOT valid (expired/tampered/malformed) -> reject immediately
 *      with 401 using JwtService's own reason, instead of silently treating it like "no
 *      token" - a client that sent a bad token deserves a specific error, not a generic one.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {
            String userId = jwtService.extractUserId(token);
            Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (InvalidTokenException ex) {
            SecurityContextHolder.clearContext();
            SecurityResponseWriter.writeUnauthorized(request, response, objectMapper, ex.getMessage());
        }
    }
}