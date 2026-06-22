package com.example.Hehe.service;

import com.example.Hehe.model.AuditLog;
import com.example.Hehe.model.User;
import com.example.Hehe.model.UserRole;
import com.example.Hehe.repository.AuditLogRepository;
import com.example.Hehe.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Triển khai Service Nhật ký hoạt động.
 * Xử lý ghi log, chống spam đăng nhập/xuất, và truy vấn log phân quyền.
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    // ─── Hằng số cho Anti-Spam ──────────────────────────────────────────────
    private static final int SPAM_WINDOW_MINUTES = 30;     // Cửa sổ thời gian đếm spam
    // TODO (DEV MODE): Đã nới lỏng giới hạn để thuận tiện cho quá trình code/test. 
    // Mốc cũ là 4 và 5. Hãy đổi lại khi đưa dự án lên Production!
    private static final int SPAM_WARN_THRESHOLD = 9999;   // Lần thứ 9999: Cắm cờ đỏ
    private static final int SPAM_BAN_THRESHOLD = 10000;   // Lần thứ 10000: Bắt đầu phạt
    private static final int BAN_LEVEL1_MINUTES = 5;       // Phạt lần 1: 5 phút
    private static final int BAN_LEVEL2_MINUTES = 20;      // Phạt lần 2: 20 phút
    private static final int BAN_LEVEL3_HOURS = 24;        // Phạt lần 3: 24 giờ
    private static final int SPAM_LEVEL2_THRESHOLD = 2;    // Lần Spam Warning thứ 2 trong ngày -> lên mức phạt 2
    private static final int SPAM_LEVEL3_THRESHOLD = 3;    // Lần Spam Warning thứ 3 trong ngày -> lên mức phạt 3

    private final AuditLogRepository auditLogRepository;
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
        // Bước 1: Kiểm tra có đang bị phạt không
        checkBanStatus(user);

        // Bước 2: Đếm số lần login/logout trong 30 phút qua
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(SPAM_WINDOW_MINUTES);

        // [CƠ CHẾ AUTO-RESET] Lấy công chuộc tội
        // Nếu user có thao tác làm việc thật (thêm, sửa, xoá...) thì xóa bỏ án tích spam trước đó
        LocalDateTime lastGoodAction = auditLogRepository.findLastLegitimateActionTime(user.getId());
        if (lastGoodAction != null && lastGoodAction.isAfter(windowStart)) {
            windowStart = lastGoodAction;
        }

        long count = auditLogRepository.countLoginLogoutSince(user.getId(), windowStart);

        if (count >= SPAM_BAN_THRESHOLD - 1) {
            // Đã đủ ngưỡng spam -> cắm cờ đỏ, leo thang phạt
            handleSpamEscalation(user);
        } else if (count >= SPAM_WARN_THRESHOLD - 1) {
            // Lần thứ 4 -> ghi log cảnh báo đỏ nhưng vẫn cho đăng nhập
            Integer branchId = resolveBranchId(user);
            AuditLog warnLog = new AuditLog(user, branchId, "SPAM_WARNING", "users",
                    String.valueOf(user.getId()),
                    "[CẢNH BÁO] Phát hiện hành vi đăng nhập/xuất bất thường. Đây là cảnh báo tự động từ hệ thống.", true);
            auditLogRepository.save(warnLog);
            // Vẫn ghi log đăng nhập bình thường
            writeLoginLog(user);
        } else {
            // Bình thường -> ghi log
            writeLoginLog(user);
        }
    }

    /**
     * Ghi hành động ĐĂNG XUẤT kèm kiểm tra spam.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogoutWithSpamCheck(User user) {
        // Kiểm tra ban (bỏ qua nếu bị ban - vẫn cho logout nhưng không ghi log thêm nếu đang bị phạt nặng)
        if (user.getBanUntil() != null && user.getBanUntil().isAfter(LocalDateTime.now())) {
            return; // Đang trong thời gian phạt, không ghi log logout spam thêm nữa
        }
        Integer branchId = resolveBranchId(user);
        AuditLog log = new AuditLog(user, branchId, "LOGOUT", "users", String.valueOf(user.getId()),
                user.getFullName() + " đã đăng xuất khỏi hệ thống.");
        auditLogRepository.save(log);
    }

    /**
     * Tìm kiếm log theo nhiều điều kiện, tự động giới hạn branchId theo phân quyền.
     */
    @Override
    public List<AuditLog> searchLogs(User currentUser, Integer filterUserId, String action,
                                      LocalDateTime from, LocalDateTime to, String keyword) {
        // Xác định branchId được phép xem: ADMIN -> 1, MANAGER/STAFF -> chi nhánh của họ
        Integer branchId;
        if (currentUser.getRole() == UserRole.ADMIN) {
            branchId = 1; // Admin chỉ xem log chi nhánh tổng
        } else {
            branchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (branchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");
        }

        return auditLogRepository.searchLogs(branchId, filterUserId, action, from, to,
                (keyword != null && keyword.isBlank()) ? null : keyword);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private void writeLoginLog(User user) {
        Integer branchId = resolveBranchId(user);
        AuditLog log = new AuditLog(user, branchId, "LOGIN", "users", String.valueOf(user.getId()),
                user.getFullName() + " đã đăng nhập vào hệ thống.");
        auditLogRepository.save(log);
    }

    /**
     * Kiểm tra xem user có đang trong thời gian bị phạt không.
     * Ném RuntimeException nếu vẫn đang bị phạt.
     */
    private void checkBanStatus(User user) {
        if (user.getBanUntil() != null && user.getBanUntil().isAfter(LocalDateTime.now())) {
            String banTime = user.getBanUntil().toString().replace("T", " ").substring(0, 16);
            throw new com.example.Hehe.exception.TooManyRequestsException(
                    "Tài khoản tạm thời bị khóa do thao tác quá nhanh. Vui lòng thử lại sau " + banTime + ".",
                    user.getBanUntil()
            );
        }
    }

    /**
     * Xử lý leo thang mức phạt khi phát hiện SPAM.
     * Đếm số lần SPAM_WARNING trong 24 giờ qua để quyết định mức phạt.
     */
    private void handleSpamEscalation(User user) {
        LocalDateTime dayStart = LocalDateTime.now().minusHours(24);

        // [CƠ CHẾ AUTO-RESET] Xóa án tích mức phạt nếu có hành động hợp lệ
        LocalDateTime lastGoodAction = auditLogRepository.findLastLegitimateActionTime(user.getId());
        if (lastGoodAction != null && lastGoodAction.isAfter(dayStart)) {
            dayStart = lastGoodAction;
        }

        long spamCount = auditLogRepository.countSpamWarningsSince(user.getId(), dayStart);

        // Ghi log cảnh báo đỏ
        Integer branchId = resolveBranchId(user);
        AuditLog warnLog = new AuditLog(user, branchId, "SPAM_WARNING", "users",
                String.valueOf(user.getId()),
                "[CẢNH BÁO SPAM] Phát hiện hành vi spam đăng nhập/xuất lần " + (spamCount + 1) + " trong ngày. Tài khoản bị phạt tạm thời.", true);
        auditLogRepository.save(warnLog);

        // Quyết định mức phạt
        LocalDateTime banUntil;
        String banMessage;

        if (spamCount >= SPAM_LEVEL3_THRESHOLD - 1) {
            // Mức 3: Phạt 24 giờ
            banUntil = LocalDateTime.now().plusHours(BAN_LEVEL3_HOURS);
            banMessage = "Tài khoản bị khóa 24 giờ do cố tình vi phạm nhiều lần.";
        } else if (spamCount >= SPAM_LEVEL2_THRESHOLD - 1) {
            // Mức 2: Phạt 20 phút
            banUntil = LocalDateTime.now().plusMinutes(BAN_LEVEL2_MINUTES);
            banMessage = "Tài khoản tạm khóa 20 phút.";
        } else {
            // Mức 1: Phạt 5 phút
            banUntil = LocalDateTime.now().plusMinutes(BAN_LEVEL1_MINUTES);
            banMessage = "Tài khoản tạm khóa 5 phút.";
        }

        // Lưu thời gian phạt vào Database
        user.setBanUntil(banUntil);
        userRepository.save(user);

        String banTimeStr = banUntil.toString().replace("T", " ").substring(0, 16);
        throw new com.example.Hehe.exception.TooManyRequestsException(
                banMessage + " Vui lòng thử lại sau " + banTimeStr + ".",
                banUntil
        );
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
