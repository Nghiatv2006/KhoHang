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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        // 1. Tìm kiếm User từ Database PostgreSQL thực tế
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản hoặc mật khẩu không chính xác"));

        System.out.println("DEBUG: login request for user: " + request.getUsername());
        System.out.println("DEBUG: request password length: " + (request.getPassword() != null ? request.getPassword().length() : "null"));
        System.out.println("DEBUG: db password hash: '" + user.getPassword() + "'");
        System.out.println("DEBUG: db password hash length: " + (user.getPassword() != null ? user.getPassword().length() : "null"));
        
        boolean isMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());
        System.out.println("DEBUG: passwordEncoder.matches result: " + isMatch);

        // 2. Kiểm tra mật khẩu (so khớp bằng BCrypt)
        if (!isMatch) {
            throw new RuntimeException("Tài khoản hoặc mật khẩu không chính xác");
        }

        // 3. Kiểm tra trạng thái tài khoản (ACTIVE hay LOCKED)
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa");
        }

        // 4. Sinh JWT Token
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());

        // 5. Lấy thông tin Chi nhánh (nếu có)
        Integer branchId = user.getBranch() != null ? user.getBranch().getId() : null;
        String branchName = user.getBranch() != null ? user.getBranch().getName() : null;

        // 6. Trả về thông tin đăng nhập thành công
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
}
