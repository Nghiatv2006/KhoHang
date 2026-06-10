package com.example.Hehe.controller;

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
}
