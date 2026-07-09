package com.example.Hehe.service;

import com.example.Hehe.dto.LoginRequest;
import com.example.Hehe.dto.LoginResponse;
import com.example.Hehe.model.User;
import com.example.Hehe.model.UserStatus;
import com.example.Hehe.repository.UserRepository;
import com.example.Hehe.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;

    private final java.util.concurrent.ConcurrentHashMap<String, Integer> failedAttempts = new java.util.concurrent.ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditLogService = auditLogService;
    }

    public LoginResponse login(LoginRequest request) {
        // 1. Tìm kiếm User từ Database PostgreSQL thực tế
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản hoặc mật khẩu không chính xác"));

        // Kiểm tra xem có đang bị chặn do đăng nhập sai không
        if (user.getBanUntil() != null && user.getBanUntil().isAfter(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Bạn đã nhập sai mật khẩu quá nhiều lần. Vui lòng thử lại sau.");
        }

        // 2. Kiểm tra mật khẩu (so khớp bằng BCrypt)
        boolean isMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!isMatch) {
            int attempts = failedAttempts.getOrDefault(user.getUsername(), 0) + 1;
            if (attempts >= 5) {
                user.setBanUntil(java.time.LocalDateTime.now().plusSeconds(30));
                userRepository.save(user);
                failedAttempts.remove(user.getUsername());
                throw new RuntimeException("Bạn đã nhập sai mật khẩu quá nhiều lần. Vui lòng thử lại sau.");
            } else {
                failedAttempts.put(user.getUsername(), attempts);
                throw new RuntimeException("Tài khoản hoặc mật khẩu không chính xác");
            }
        }
        
        // Đăng nhập thành công, reset đếm sai
        failedAttempts.remove(user.getUsername());

        // 3. Kiểm tra trạng thái tài khoản (ACTIVE hay LOCKED)
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa");
        }

        // 4. Kiểm tra và ghi log đăng nhập (có chống spam)
        // Nếu đang bị phạt, auditLogService sẽ tự động throw TooManyRequestsException
        auditLogService.logLoginWithSpamCheck(user);

        // 5. Sinh JWT Token
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());

        // 6. Lấy thông tin Chi nhánh (nếu có)
        Integer branchId = user.getBranch() != null ? user.getBranch().getId() : null;
        String branchName = user.getBranch() != null ? user.getBranch().getName() : null;

        // 7. Trả về thông tin đăng nhập thành công
        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name(),
                branchId,
                branchName,
                user.getStatus().name()
        );
    }

    /**
     * Ghi log đăng xuất cho user hiện tại.
     */
    public void logout(User user) {
        auditLogService.logLogoutWithSpamCheck(user);
    }
}
