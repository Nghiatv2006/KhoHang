package com.example.Hehe.repository;

import com.example.Hehe.model.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Integer> {
    Optional<PasswordResetOtp> findFirstByUsernameAndOtpCodeAndUsedFalseAndExpiryTimeAfter(
        String username, String otpCode, LocalDateTime now
    );

    List<PasswordResetOtp> findByUsernameAndUsedFalse(String username);
}
