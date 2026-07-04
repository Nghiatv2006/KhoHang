package com.example.Hehe.security;

import com.example.Hehe.model.User;
import com.example.Hehe.model.UserStatus;
import com.example.Hehe.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromCookie(request);
            System.out.println("DEBUG JwtAuthenticationFilter: Extracting cookie jwt = " + (jwt != null ? (jwt.substring(0, 15) + "...") : "null"));

            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                System.out.println("DEBUG JwtAuthenticationFilter: Valid JWT for username = " + username);

                // Load user từ DB để kiểm tra trạng thái hoạt động thực tế
                User user = userRepository.findByUsername(username).orElse(null);
                
                if (user != null) {
                    System.out.println("DEBUG JwtAuthenticationFilter: Found user in DB, role = " + user.getRole() + ", status = " + user.getStatus());
                    if (user.getStatus() == UserStatus.LOCKED) {
                        System.out.println("DEBUG JwtAuthenticationFilter: User is LOCKED, returning 401");
                        // Trả về 401 ngay lập tức nếu tài khoản bị khóa
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write("{\"message\": \"Tài khoản của bạn đã bị khóa và không thể thực hiện thao tác này.\"}");
                        return;
                    }

                    // --- Sliding Window Token Refresh ---
                    // Tự động gia hạn token nếu sắp hết hạn (còn dưới 12 giờ)
                    try {
                        java.util.Date expiration = jwtTokenProvider.getExpirationDateFromToken(jwt);
                        long remainingMs = expiration.getTime() - System.currentTimeMillis();
                        long thresholdMs = 12 * 60 * 60 * 1000L; // 12 giờ

                        if (remainingMs < thresholdMs) {
                            String newJwt = jwtTokenProvider.generateToken(username, user.getRole().name());
                            org.springframework.http.ResponseCookie newCookie = org.springframework.http.ResponseCookie.from("accessToken", newJwt)
                                    .httpOnly(true)
                                    .secure(false) // chạy local http
                                    .path("/")
                                    .maxAge(86400) // 24 giờ
                                    .sameSite("Lax")
                                    .build();
                            response.setHeader(org.springframework.http.HttpHeaders.SET_COOKIE, newCookie.toString());
                            System.out.println("DEBUG JwtAuthenticationFilter: Reissued JWT cookie due to low remaining duration (" + (remainingMs / 1000 / 60) + " minutes left)");
                        }
                    } catch (Exception e) {
                        System.err.println("DEBUG JwtAuthenticationFilter: Failed to auto-refresh JWT: " + e.getMessage());
                    }
                    // ------------------------------------

                    // Thiết lập quyền dựa trên vai trò (ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF)
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user, null, Collections.singletonList(authority));
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("DEBUG JwtAuthenticationFilter: Authentication set in SecurityContext");
                } else {
                    System.out.println("DEBUG JwtAuthenticationFilter: User not found in DB");
                }
            } else {
                if (jwt != null) {
                    System.out.println("DEBUG JwtAuthenticationFilter: JWT validation failed");
                }
            }
        } catch (Exception ex) {
            System.err.println("DEBUG JwtAuthenticationFilter ERROR: " + ex.getMessage());
            ex.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
