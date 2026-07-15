package com.example.Hehe.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpMail(String toEmail, String username, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[WareHub] Mã OTP xác nhận khôi phục mật khẩu");

            String htmlContent = "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 12px; background-color: #ffffff;\">" +
                    "  <div style=\"text-align: center; margin-bottom: 20px; border-bottom: 2px solid #f5f5f5; padding-bottom: 15px;\">" +
                    "    <h2 style=\"color: #d63031; margin: 0;\">WareHub Enterprise</h2>" +
                    "    <p style=\"color: #7f8c8d; font-size: 0.9rem; margin: 5px 0 0 0;\">Hệ thống quản lý kho hàng đa chi nhánh</p>" +
                    "  </div>" +
                    "  <div style=\"padding: 10px 0;\">" +
                    "    <p style=\"font-size: 1rem; color: #2d3436; line-height: 1.6;\">Chào bạn,</p>" +
                    "    <p style=\"font-size: 1rem; color: #2d3436; line-height: 1.6;\">Bạn đã yêu cầu khôi phục mật khẩu cho tài khoản <strong>" + username + "</strong> trên hệ thống WareHub.</p>" +
                    "    <div style=\"text-align: center; margin: 30px 0; padding: 15px; background-color: #fff9db; border: 1px dashed #f59f00; border-radius: 8px;\">" +
                    "      <p style=\"font-size: 0.9rem; color: #f59f00; font-weight: bold; margin: 0 0 10px 0;\">MÃ OTP CỦA BẠN</p>" +
                    "      <span style=\"font-size: 2.2rem; font-family: monospace; font-weight: 800; letter-spacing: 5px; color: #d63031;\">" + otpCode + "</span>" +
                    "    </div>" +
                    "    <p style=\"font-size: 0.95rem; color: #e74c3c; font-weight: 600; line-height: 1.6;\">Lưu ý: Mã OTP này có hiệu lực trong vòng 5 phút và chỉ sử dụng được 1 lần. Vui lòng tuyệt đối không chia sẻ mã này cho bất kỳ ai.</p>" +
                    "  </div>" +
                    "  <div style=\"margin-top: 30px; border-top: 1px solid #f5f5f5; padding-top: 15px; font-size: 0.85rem; color: #95a5a6; text-align: center;\">" +
                    "    <p style=\"margin: 0;\">Đây là email tự động từ hệ thống WareHub. Vui lòng không trả lời email này.</p>" +
                    "    <p style=\"margin: 5px 0 0 0;\">&copy; 2026 WareHub Team. All rights reserved.</p>" +
                    "  </div>" +
                    "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Lỗi gửi email OTP: " + e.getMessage(), e);
        }
    }
    public void sendVerificationMail(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[WareHub] Xác thực địa chỉ email");

            String htmlContent = "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 12px; background-color: #ffffff;\">" +
                    "  <div style=\"text-align: center; margin-bottom: 20px; border-bottom: 2px solid #f5f5f5; padding-bottom: 15px;\">" +
                    "    <h2 style=\"color: #4361ee; margin: 0;\">WareHub Enterprise</h2>" +
                    "    <p style=\"color: #7f8c8d; font-size: 0.9rem; margin: 5px 0 0 0;\">Hệ thống quản lý kho hàng đa chi nhánh</p>" +
                    "  </div>" +
                    "  <div style=\"padding: 10px 0;\">" +
                    "    <p style=\"font-size: 1rem; color: #2d3436; line-height: 1.6;\">Chào bạn,</p>" +
                    "    <p style=\"font-size: 1rem; color: #2d3436; line-height: 1.6;\">Đây là email kiểm tra tính hợp lệ của hòm thư từ hệ thống WareHub.</p>" +
                    "    <p style=\"font-size: 1rem; color: #2d3436; line-height: 1.6;\">Nếu bạn nhận được email này, có nghĩa là quản trị viên hệ thống đang tiến hành cấu hình tài khoản của bạn.</p>" +
                    "  </div>" +
                    "  <div style=\"margin-top: 30px; border-top: 1px solid #f5f5f5; padding-top: 15px; font-size: 0.85rem; color: #95a5a6; text-align: center;\">" +
                    "    <p style=\"margin: 0;\">Đây là email tự động từ hệ thống WareHub. Vui lòng không trả lời email này.</p>" +
                    "    <p style=\"margin: 5px 0 0 0;\">&copy; 2026 WareHub Team. All rights reserved.</p>" +
                    "  </div>" +
                    "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Lỗi gửi email xác thực: " + e.getMessage(), e);
        }
    }
}
