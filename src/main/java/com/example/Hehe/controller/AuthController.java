package com.example.Hehe.controller;

import com.example.Hehe.dto.LoginRequest;
import com.example.Hehe.dto.LoginResponse;
import com.example.Hehe.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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

            // 2. Trả về thông tin user (không kèm token trong Body nữa) và đính kèm Cookie vào Header
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
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
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
