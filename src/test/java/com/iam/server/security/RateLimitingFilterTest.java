package com.iam.server.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.iam.server.service.RateLimitingService;

class RateLimitingFilterTest {

    private RateLimitingService rateLimitingService;
    private RateLimitingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitingService = mock(RateLimitingService.class);
        filter = new RateLimitingFilter(rateLimitingService);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void testFilter_allowsRequestWhenWithinLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitingService.isAllowed(anyString(), anyInt(), any())).thenReturn(true);
        when(rateLimitingService.getRemainingRequests(anyString(), anyInt())).thenReturn(4L);
        when(rateLimitingService.getResetSeconds(anyString(), any())).thenReturn(55L);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertEquals("5", response.getHeader("X-RateLimit-Limit"));
        assertEquals("4", response.getHeader("X-RateLimit-Remaining"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testFilter_blocksRequestWith429WhenRateLimitExceeded() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitingService.isAllowed(anyString(), anyInt(), any())).thenReturn(false);
        when(rateLimitingService.getRemainingRequests(anyString(), anyInt())).thenReturn(0L);
        when(rateLimitingService.getResetSeconds(anyString(), any())).thenReturn(45L);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertEquals("45", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("Too many requests"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testFilter_ignoresNonSensitiveEndpoints() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitingService, never()).isAllowed(anyString(), anyInt(), any());
        verify(filterChain).doFilter(request, response);
    }
}
