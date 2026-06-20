package com.example.Hehe.controller;

import com.example.Hehe.dto.LoginRequest;
import com.example.Hehe.dto.LoginResponse;
import com.example.Hehe.exception.TooManyRequestsException;
import com.example.Hehe.model.User;
import com.example.Hehe.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            
            // 1. Tạo Cookie chứa Token JWT
            ResponseCookie jwtCookie = ResponseCookie.from("accessToken", response.getToken())
                    .httpOnly(true)            // Cấm JS đọc cookie này (Chống XSS)
                    .secure(false)             // Set true nếu chạy HTTPS (chạy local http tạm thời để false)
                    .path("/")                 // Có hiệu lực cho toàn bộ API
                    .maxAge(86400)             // Hết hạn sau 24h (tính bằng giây)
                    .sameSite("Lax")           // Đổi sang Lax để tránh lỗi mất cookie khi chuyển trang SPA
                    .build();

            // 2. Trả về thông tin user và đính kèm Cookie
            Map<String, Object> body = new HashMap<>();
            body.put("id", response.getId());
            body.put("username", response.getUsername());
            body.put("fullName", response.getFullName());
            body.put("role", response.getRole());
            body.put("branchId", response.getBranchId() != null ? response.getBranchId() : "");
            body.put("branchName", response.getBranchName() != null ? response.getBranchName() : "");
            body.put("status", response.getStatus());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(body);
        } catch (TooManyRequestsException ex) {
            // Trả về HTTP 429 khi bị phạt spam
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "message", ex.getMessage(),
                    "banUntil", ex.getBanUntil() != null ? ex.getBanUntil().toString() : ""
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal User currentUser) {
        // Ghi log đăng xuất nếu user đã đăng nhập
        if (currentUser != null) {
            try {
                authService.logout(currentUser);
            } catch (Exception ignored) {}
        }
        // Xoá cookie bằng cách set maxAge = 0
        ResponseCookie deleteCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(Map.of("message", "Đăng xuất thành công"));
    }
}
