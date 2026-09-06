package com.iam.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        rateLimitingFilter = new RateLimitingFilter();
    }

    @Test
    void doFilter_allowsRequestsUnderLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        rateLimitingFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilter_blocksRequestsExceedingLimit() throws ServletException, IOException {
        String clientIp = "192.168.1.200";

        // First 10 requests should succeed
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = new MockFilterChain();
            rateLimitingFilter.doFilter(request, response, filterChain);
            assertEquals(200, response.getStatus());
        }

        // 11th request must be blocked with HTTP 429 Too Many Requests
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();
        rateLimitingFilter.doFilter(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
    }

    @Test
    void doFilter_doesNotRateLimitNonAuthPaths() throws ServletException, IOException {
        String clientIp = "192.168.1.300";

        // Requests to other paths should not be restricted by this filter
        for (int i = 0; i < 15; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/profile");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = new MockFilterChain();
            rateLimitingFilter.doFilter(request, response, filterChain);
            assertEquals(200, response.getStatus());
        }
    }
}
