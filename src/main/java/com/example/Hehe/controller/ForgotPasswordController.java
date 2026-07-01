package com.example.Hehe.controller;

import com.example.Hehe.service.ForgotPasswordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/forgot-password")
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    public ForgotPasswordController(ForgotPasswordService forgotPasswordService) {
        this.forgotPasswordService = forgotPasswordService;
    }

    @GetMapping("/find-accounts")
    public ResponseEntity<?> findAccounts(@RequestParam String email) {
        try {
            List<Map<String, String>> accounts = forgotPasswordService.findAccountsByEmail(email);
            return ResponseEntity.ok(accounts);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody SendOtpRequest request) {
        try {
            forgotPasswordService.sendOtp(request.getUsername(), request.getEmail());
            return ResponseEntity.ok(Map.of("message", "Mã OTP đã được gửi tới email của bạn."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        try {
            forgotPasswordService.verifyOtp(request.getUsername(), request.getEmail(), request.getOtp());
            return ResponseEntity.ok(Map.of("message", "Xác minh OTP thành công."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            forgotPasswordService.resetPassword(request.getUsername(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    public static class SendOtpRequest {
        private String username;
        private String email;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class VerifyOtpRequest {
        private String username;
        private String email;
        private String otp;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }

    public static class ResetPasswordRequest {
        private String username;
        private String otp;
        private String newPassword;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
