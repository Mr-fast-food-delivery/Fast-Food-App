package com.phegon.FoodApp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class MockJwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // ----------- CASE 1: Không có token → 401 -----------
        if (header == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (!header.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = header.substring(7);

        // ----------- CASE 2: Token expired / invalid → 401 -----------
        if (token.equals("expiredToken") || token.equals("invalid") || token.equals("bad")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // ========== CASE 3: Token hợp lệ → set Authentication ==========
        // Mapping token → role giống như test mong muốn
        UsernamePasswordAuthenticationToken auth = null;

        switch (token) {
            case "adminToken":
                auth = new UsernamePasswordAuthenticationToken(
                        "admin@test.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ADMIN"))
                );
                break;

            case "customerToken":
                auth = new UsernamePasswordAuthenticationToken(
                        "customer@test.com",
                        null,
                        List.of(new SimpleGrantedAuthority("CUSTOMER"))
                );
                break;

            case "inactiveUserToken":
                auth = new UsernamePasswordAuthenticationToken(
                        "inactive@test.com",
                        null,
                        List.of(new SimpleGrantedAuthority("CUSTOMER"))
                );
                break;

            case "deletedUserToken":
                auth = new UsernamePasswordAuthenticationToken(
                        "deleted@test.com",
                        null,
                        List.of(new SimpleGrantedAuthority("CUSTOMER"))
                );
                break;

            default:
                // token hợp lệ khác -> default role CUSTOMER
                auth = new UsernamePasswordAuthenticationToken(
                        "test@test.com",
                        null,
                        List.of(new SimpleGrantedAuthority("CUSTOMER"))
                );
        }

        // Gán vào SecurityContext
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
