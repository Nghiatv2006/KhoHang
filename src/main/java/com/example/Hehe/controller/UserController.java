package com.example.Hehe.controller;

import com.example.Hehe.dto.ChangePasswordRequest;
import com.example.Hehe.dto.UserResponse;
import com.example.Hehe.dto.UserSaveRequest;
import com.example.Hehe.model.User;
import com.example.Hehe.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        return ResponseEntity.ok(Map.of(
                "id", currentUser.getId(),
                "username", currentUser.getUsername(),
                "fullName", currentUser.getFullName(),
                "role", currentUser.getRole(),
                "branchId", currentUser.getBranch() != null ? currentUser.getBranch().getId() : "",
                "branchName", currentUser.getBranch() != null ? currentUser.getBranch().getName() : "",
                "status", currentUser.getStatus()
        ));
    }

    @GetMapping
    public ResponseEntity<?> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User currentUser) {
        try {
            List<UserResponse> users = userService.searchUsers(keyword, role, branchId, status, currentUser);
            return ResponseEntity.ok(users);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestBody UserSaveRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            UserResponse response = userService.createUser(request, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Integer id,
            @RequestBody UserSaveRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            UserResponse response = userService.updateUser(id, request, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        try {
            userService.deleteUser(id, currentUser);
            return ResponseEntity.ok(Map.of("message", "Xóa người dùng thành công."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        try {
            UserResponse response = userService.toggleUserStatus(id, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
    @PutMapping("/me/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            userService.changePassword(request, currentUser);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                throw new RuntimeException("Email không được để trống.");
            }
            
            // Validate email via AbstractAPI
            boolean exists = isEmailExistsViaAPI(email.trim());
            if (!exists) {
                throw new RuntimeException("Tài khoản email này không tồn tại thật trên hệ thống máy chủ (như Google).");
            }
            
            return ResponseEntity.ok(Map.of("message", "Đã xác thực email thành công (Email có thật)."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    private boolean isEmailExistsViaAPI(String email) {
        String apiKey = System.getenv("ABSTRACT_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("Chưa cấu hình ABSTRACT_API_KEY trong file .env");
        }
        
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String url = "https://emailreputation.abstractapi.com/v1/?api_key=" + apiKey + "&email=" + email;
            java.util.Map<String, Object> response = restTemplate.getForObject(url, java.util.Map.class);
            
            if (response != null && response.containsKey("email_deliverability")) {
                java.util.Map<String, Object> deliverability = (java.util.Map<String, Object>) response.get("email_deliverability");
                if (deliverability != null) {
                    String status = (String) deliverability.get("status");
                    return "deliverable".equalsIgnoreCase(status);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi gọi AbstractAPI: " + e.getMessage());
        }
        return false;
    }
}
