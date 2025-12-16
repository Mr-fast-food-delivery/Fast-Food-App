package com.phegon.FoodApp.security;

import com.phegon.FoodApp.exceptions.CustomAuthenticationEntryPoint;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * ✅ BỎ QUA JWT FILTER CHO AUTH API
     * - Login / Register KHÔNG cần JWT
     * - Tránh filter nuốt request login
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/")
            || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = getTokenFromRequest(request);

            if (token != null) {
                // 1️⃣ Parse email từ JWT
                String email = jwtUtils.getUsernameFromToken(token);

                // 2️⃣ Load user từ DB
                UserDetails userDetails =
                        customUserDetailsService.loadUserByUsername(email);

                // 3️⃣ Validate token
                if (StringUtils.hasText(email)
                        && jwtUtils.isTokenValid(token, userDetails)) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }
            }

            // 4️⃣ Cho request đi tiếp (BẮT BUỘC)
            filterChain.doFilter(request, response);

        }
        catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("JWT expired", ex)
            );
        }
        catch (AuthenticationException ex) {
            log.warn("Authentication error: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, ex);
        }
        catch (Exception ex) {
            log.error("Unexpected authentication error", ex);
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Authentication failed", ex)
            );
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
