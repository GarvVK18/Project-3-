package com.iam.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        // Test in-memory fallback first
        rateLimitingFilter = new RateLimitingFilter(null);
    }

    @Test
    void doFilter_allowsUpToFiveRequestsAndBlocksSixth() throws ServletException, IOException {
        String clientIp = "10.0.0.1";

        // First 5 requests must pass
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = new MockFilterChain();

            rateLimitingFilter.doFilter(request, response, filterChain);

            assertEquals(200, response.getStatus());
            assertEquals("5", response.getHeader("X-RateLimit-Limit"));
            assertNotNull(response.getHeader("X-RateLimit-Remaining"));
        }

        // 6th request must be rejected with 429 and Retry-After header
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        rateLimitingFilter.doFilter(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertNotNull(response.getHeader("Retry-After"));
        assertEquals("60", response.getHeader("Retry-After"));
    }

    @Test
    void doFilter_withRedis_incrementsRedisCounterAndSetsHeaders() throws ServletException, IOException {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redisTemplate.getExpire(anyString())).thenReturn(59L);

        RateLimitingFilter redisFilter = new RateLimitingFilter(redisTemplate);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        redisFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertEquals("5", response.getHeader("X-RateLimit-Limit"));
        assertEquals("4", response.getHeader("X-RateLimit-Remaining"));
        assertEquals("59", response.getHeader("X-RateLimit-Reset"));
        verify(valueOps).increment("ratelimit:10.0.0.2");
    }

    @Test
    void doFilter_withRedisExceeded_returns429() throws ServletException, IOException {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(6L);
        when(redisTemplate.getExpire(anyString())).thenReturn(42L);

        RateLimitingFilter redisFilter = new RateLimitingFilter(redisTemplate);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.3");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        redisFilter.doFilter(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertEquals("42", response.getHeader("Retry-After"));
        assertEquals("5", response.getHeader("X-RateLimit-Limit"));
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
    }
}
