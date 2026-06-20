package com.example.Hehe.service;

import com.example.Hehe.model.AuditLog;
import com.example.Hehe.model.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service xử lý Nhật ký hoạt động (Audit Log).
 */
public interface AuditLogService {

    /**
     * Ghi một hành động vào nhật ký.
     * @param user       Người thực hiện hành động
     * @param action     Mã hành động: LOGIN, LOGOUT, CREATE, UPDATE, DELETE, APPROVE, CANCEL, SPAM_WARNING, LOCK_ACCOUNT
     * @param entityName Tên thực thể bị tác động (products, receipts, users...)
     * @param entityId   ID của thực thể (để tra cứu ngược)
     * @param details    Mô tả chi tiết bằng Tiếng Việt
     */
    void logAction(User user, String action, String entityName, String entityId, String details);

    /**
     * Ghi hành động ĐĂNG NHẬP, đồng thời kiểm tra và xử lý chống Spam.
     * Ném TooManyRequestsException nếu user đang trong thời gian bị phạt.
     * @param user Người đăng nhập
     */
    void logLoginWithSpamCheck(User user);

    /**
     * Ghi hành động ĐĂNG XUẤT, đồng thời kiểm tra chống Spam.
     * @param user Người đăng xuất
     */
    void logLogoutWithSpamCheck(User user);

    /**
     * Tìm kiếm log theo nhiều điều kiện lọc (dành cho giao diện quản lý).
     * Tự động giới hạn branchId theo phân quyền của currentUser.
     */
    List<AuditLog> searchLogs(User currentUser, Integer filterUserId, String action,
                               LocalDateTime from, LocalDateTime to, String keyword);
}
