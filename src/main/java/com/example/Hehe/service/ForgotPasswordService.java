package com.example.Hehe.service;

import com.example.Hehe.model.PasswordResetOtp;
import com.example.Hehe.model.User;
import com.example.Hehe.repository.PasswordResetOtpRepository;
import com.example.Hehe.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ForgotPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public ForgotPasswordService(UserRepository userRepository,
                                 PasswordResetOtpRepository passwordResetOtpRepository,
                                 MailService mailService,
                                 PasswordEncoder passwordEncoder,
                                 AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public List<Map<String, String>> findAccountsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }
        List<User> users = userRepository.findByEmail(email.trim());
        List<Map<String, String>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, String> map = new HashMap<>();
            map.put("username", u.getUsername());
            map.put("fullName", u.getFullName());
            result.add(map);
        }
        return result;
    }

    @Transactional
    public void sendOtp(String username, String email) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Tên đăng nhập không được để trống");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }

        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email.trim())) {
            throw new RuntimeException("Email cung cấp không khớp với tài khoản");
        }

        // Vô hiệu hóa toàn bộ OTP cũ chưa sử dụng của tài khoản này
        List<PasswordResetOtp> oldOtps = passwordResetOtpRepository.findByUsernameAndUsedFalse(user.getUsername());
        if (!oldOtps.isEmpty()) {
            for (PasswordResetOtp oldOtp : oldOtps) {
                oldOtp.setUsed(true);
            }
            passwordResetOtpRepository.saveAll(oldOtps);
        }

        String otpCode = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setUsername(user.getUsername());
        otp.setEmail(user.getEmail());
        otp.setOtpCode(otpCode);
        otp.setExpiryTime(expiryTime);
        otp.setUsed(false);
        passwordResetOtpRepository.save(otp);

        mailService.sendOtpMail(user.getEmail(), user.getUsername(), otpCode);
    }

    public void verifyOtp(String username, String email, String otp) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Tên đăng nhập không được để trống");
        }
        if (otp == null || otp.trim().isEmpty()) {
            throw new RuntimeException("Mã OTP không được để trống");
        }

        Optional<PasswordResetOtp> otpOpt = passwordResetOtpRepository
                .findFirstByUsernameAndOtpCodeAndUsedFalseAndExpiryTimeAfter(
                        username.trim(), otp.trim(), LocalDateTime.now()
                );

        if (otpOpt.isEmpty()) {
            throw new RuntimeException("Mã OTP không chính xác, đã sử dụng hoặc đã hết hạn");
        }
    }

    @Transactional
    public void resetPassword(String username, String otp, String newPassword) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Tên đăng nhập không được để trống");
        }
        if (otp == null || otp.trim().isEmpty()) {
            throw new RuntimeException("Mã OTP không được để trống");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu mới không được để trống");
        }

        PasswordResetOtp otpRecord = passwordResetOtpRepository
                .findFirstByUsernameAndOtpCodeAndUsedFalseAndExpiryTimeAfter(
                        username.trim(), otp.trim(), LocalDateTime.now()
                )
                .orElseThrow(() -> new RuntimeException("Mã OTP không chính xác, đã sử dụng hoặc đã hết hạn"));

        otpRecord.setUsed(true);
        passwordResetOtpRepository.save(otpRecord);

        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        try {
            auditLogService.logAction(user, "UPDATE", "users", String.valueOf(user.getId()),
                    "Khôi phục mật khẩu thành công qua mã OTP.");
        } catch (Exception e) {
            // Bỏ qua lỗi audit log nếu có để đảm bảo transaction hoàn thành
        }
    }

    private String generateOtp() {
        Random random = new Random();
        int number = random.nextInt(900000) + 100000;
        return String.valueOf(number);
    }
}
