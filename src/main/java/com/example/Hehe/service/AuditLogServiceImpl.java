package com.example.Hehe.service;

import com.example.Hehe.model.AuditLog;
import com.example.Hehe.model.User;
import com.example.Hehe.model.UserRole;
import com.example.Hehe.repository.AuditLogRepository;
import com.example.Hehe.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Triển khai Service Nhật ký hoạt động.
 * Xử lý ghi log, chống spam đăng nhập/xuất, và truy vấn log phân quyền.
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    // ─── Hằng số cho Anti-Spam ──────────────────────────────────────────────
    private static final int SPAM_WINDOW_SECONDS = 10;     // Cửa sổ thời gian: 10 giây
    private static final int SPAM_WARN_THRESHOLD = 5;      // 5 lần miễn phí, lần thứ 6 bị khóa
    
    // Cấu hình số giây cấm (BAN_SECONDS)
    private static final int BAN_SECONDS = 3;

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    
    @SuppressWarnings("unused")
    private final UserRepository userRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Ghi một dòng log đơn giản vào Database.
     * Dùng REQUIRES_NEW để log luôn được ghi kể cả khi transaction cha bị rollback.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(User user, String action, String entityName, String entityId, String details) {
        Integer branchId = resolveBranchId(user);
        AuditLog log = new AuditLog(user, branchId, action, entityName, entityId, details);
        auditLogRepository.save(log);
    }

    /**
     * Ghi hành động ĐĂNG NHẬP kèm kiểm tra spam.
     * Ném ngoại lệ với HTTP 429 nếu user đang bị phạt.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginWithSpamCheck(User user) {
        // Bước 1: Kiểm tra xem tài khoản có đang trong thời gian bị khóa (3s) không
        checkBanStatus(user);

        // Bước 2: Kiểm tra xem thao tác lần này có vượt ngưỡng 5 lần / 10s không
        checkAndHandleSpam(user);

        // Bước 3: Nếu an toàn thì ghi log đăng nhập bình thường
        writeLoginLog(user);
    }

    /**
     * Ghi hành động ĐĂNG XUẤT kèm kiểm tra spam.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogoutWithSpamCheck(User user) {
        // Không bắt lỗi cấm (để user được logout), nhưng vẫn chặn đếm spam
        // if (user.getBanUntil() != null && user.getBanUntil().isAfter(LocalDateTime.now())) {
        //     return;
        // }
        checkAndHandleSpam(user);


        Integer branchId = resolveBranchId(user);
        AuditLog log = new AuditLog(user, branchId, "LOGOUT", "users", String.valueOf(user.getId()),
                user.getFullName() + " đã đăng xuất khỏi hệ thống.");
        auditLogRepository.save(log);
    }

    /**
     * Tìm kiếm log theo nhiều điều kiện, tự động giới hạn branchId theo phân quyền.
     */
    @Override
    public org.springframework.data.domain.Page<AuditLog> searchLogs(User currentUser, Integer filterUserId, String action,
                                      LocalDateTime from, LocalDateTime to, String keyword, org.springframework.data.domain.Pageable pageable) {
        // Xác định branchId được phép xem: ADMIN -> 1, MANAGER/STAFF -> chi nhánh của họ
        Integer branchId;
        if (currentUser.getRole() == UserRole.ADMIN) {
            branchId = 1; // Admin chỉ xem log chi nhánh tổng
        } else {
            branchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (branchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");
        }

        return auditLogRepository.searchLogs(branchId, filterUserId, action, from, to,
                (keyword != null && keyword.isBlank()) ? null : keyword, pageable);
    }

    /**
     * Tự động chạy dọn dẹp các log cũ hơn 1 năm.
     * Chạy lúc 00:00:00 mỗi ngày.
     */
    @Override
    @Transactional
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupOldLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(1);
        int deletedCount = auditLogRepository.deleteByCreatedAtBefore(cutoffDate);
        
        if (deletedCount > 0) {
            AuditLog systemLog = new AuditLog(null, 1, "DỌN DẸP", "hệ thống", "", 
                "Hệ thống đã tự động dọn dẹp vĩnh viễn " + deletedCount + " bản ghi nhật ký quá 1 năm.");
            auditLogRepository.save(systemLog);
        }
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private void writeLoginLog(User user) {
        Integer branchId = resolveBranchId(user);
        AuditLog log = new AuditLog(user, branchId, "LOGIN", "users", String.valueOf(user.getId()),
                user.getFullName() + " đã đăng nhập vào hệ thống.");
        auditLogRepository.save(log);
    }

    /**
     * Kiểm tra xem user có đang trong thời gian bị phạt 3s không.
     * Ném RuntimeException nếu vẫn đang bị phạt.
     */
    private void checkBanStatus(User user) {
        // [DEV TEST] Tạm comment để không cấm đăng nhập khi đang test
        // if (user.getBanUntil() != null && user.getBanUntil().isAfter(LocalDateTime.now())) {
        //     throw new com.example.Hehe.exception.TooManyRequestsException(
        //             "Tài khoản đang bị khóa do thao tác quá nhanh. Vui lòng đợi 3 giây.",
        //             user.getBanUntil()
        //     );
        // }
    }

    /**
     * Kiểm tra số lần thao tác trong 10s gần nhất. Nếu >= 5, ném lỗi phạt 3s.
     */
    private void checkAndHandleSpam(User user) {
        LocalDateTime windowStart = LocalDateTime.now().minusSeconds(SPAM_WINDOW_SECONDS);
        long recentActions = auditLogRepository.countLoginLogoutSince(user.getId(), windowStart);

        if (recentActions >= SPAM_WARN_THRESHOLD) {
            handleSpamEscalation(user);
        }
    }

    /**
     * Xử lý phạt khi vi phạm:
     * - Khóa tài khoản 3 giây.
     * - Ghi log cảnh báo ĐỎ (SPAM_WARNING).
     */
    private void handleSpamEscalation(User user) {
        // Ghi log cảnh báo đỏ
        Integer branchId = resolveBranchId(user);
        AuditLog warnLog = new AuditLog(user, branchId, "SPAM_WARNING", "users",
                String.valueOf(user.getId()),
                "[CẢNH BÁO] Phát hiện thao tác quá nhanh. Đã ghi nhận cờ đỏ (Đang tắt khóa 3s để test).", true);
        auditLogRepository.save(warnLog);

        // [DEV TEST] Tạm comment Phạt Khóa tài khoản 3 giây và Exception để đỡ phải chờ
        // LocalDateTime banUntil = LocalDateTime.now().plusSeconds(BAN_SECONDS);
        // user.setBanUntil(banUntil);
        // userRepository.save(user);

        // throw new com.example.Hehe.exception.TooManyRequestsException(
        //         "Thao tác quá nhiều lần! Tài khoản bị khóa tạm thời 3 giây.",
        //         banUntil
        // );
    }

    /**
     * Xác định branchId cho log.
     * ADMIN -> 1 (Chi nhánh tổng). MANAGER/STAFF -> chi nhánh của họ.
     */
    private Integer resolveBranchId(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return 1;
        }
        return user.getBranch() != null ? user.getBranch().getId() : 1;
    }
}
