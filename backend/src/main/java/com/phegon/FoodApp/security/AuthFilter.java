package com.phegon.FoodApp.security;

import com.phegon.FoodApp.exceptions.CustomAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
// @Profile("!test")
public class AuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (token != null) {
            try {
                // 🧩 Giải mã email từ token
                String email = jwtUtils.getUsernameFromToken(token);

                // 🧩 Load user từ DB
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                // 🧩 Kiểm tra token hợp lệ và set authentication
                if (StringUtils.hasText(email) && jwtUtils.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities()
                            );
                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }

            } catch (io.jsonwebtoken.ExpiredJwtException ex) {
                // ❌ Token hết hạn
                log.warn("JWT expired: {}", ex.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"statusCode\":401,\"message\":\"JWT expired\"}");
                return;

            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
                // ❌ User đã bị xóa khỏi DB hoặc không tồn tại
                log.warn("User not found or deleted: {}", ex.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"statusCode\":401,\"message\":\"User not found or deleted\"}");
                return;

            } catch (Exception ex) {
                // ❌ Các lỗi khác (ví dụ token sai format, signature lỗi,…)
                log.error("JWT parse error: {}", ex.getMessage());
                AuthenticationException authEx = new BadCredentialsException(ex.getMessage());
                customAuthenticationEntryPoint.commence(request, response, authEx);
                return;
            }
        }

        // ✅ Nếu không có token hoặc không gặp lỗi thì cho qua tiếp
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Filter error: {}", e.getMessage());
        }
    }




    private String getTokenFromRequest(HttpServletRequest request) {
        String tokenWithBearer = request.getHeader("Authorization");
        if (tokenWithBearer != null && tokenWithBearer.startsWith("Bearer ")) {
            return tokenWithBearer.substring(7);
        }
        return null;
    }

}











