package com.example.banking.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.banking.exception.InvalidTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        filter = new JwtAuthenticationFilter(jwtService, objectMapper);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthorizationHeader_continuesChain_withoutSettingAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void headerWithoutBearerPrefix_continuesChain_withoutSettingAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validToken_setsAuthenticationWithUserIdAsPrincipal_thenContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtService.extractUserId("valid.token.here")).thenReturn("user-123");

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("user-123", authentication.getName());
        assertTrue(authentication.isAuthenticated());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidToken_doesNotContinueChain_andWrites401Json() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer tampered.token.here");
        when(request.getRequestURI()).thenReturn("/api/accounts/123");
        when(jwtService.extractUserId("tampered.token.here"))
                .thenThrow(new InvalidTokenException("Token is invalid"));
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(401);
        assertTrue(stringWriter.toString().contains("Token is invalid"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void expiredToken_doesNotContinueChain_andWrites401JsonWithExpiredMessage() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.token.here");
        when(request.getRequestURI()).thenReturn("/api/accounts/123");
        when(jwtService.extractUserId("expired.token.here"))
                .thenThrow(new InvalidTokenException("Token has expired"));
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("Token has expired"));
    }
}