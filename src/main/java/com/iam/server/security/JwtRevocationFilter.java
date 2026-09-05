package com.iam.server.security;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.iam.server.service.TokenRevocationService;

@Component
public class JwtRevocationFilter extends OncePerRequestFilter {

    private final TokenRevocationService tokenRevocationService;

    public JwtRevocationFilter(TokenRevocationService tokenRevocationService) {
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && (authHeader.startsWith("Bearer ") || authHeader.startsWith("bearer "))) {
            String token = authHeader.substring(7).trim();
            if (tokenRevocationService.isTokenRevoked(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Token has been revoked or invalidated across devices\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
