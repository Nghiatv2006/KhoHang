package com.example.Hehe.repository;

import com.example.Hehe.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Lấy tất cả log theo branchId, sắp xếp mới nhất lên trước.
     */
    List<AuditLog> findByBranchIdOrderByCreatedAtDesc(Integer branchId);

    /**
     * Đếm số lần đăng nhập/xuất của user trong khoảng thời gian cho trước.
     * Dùng để kiểm tra SPAM.
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.user.id = :userId AND a.action IN ('LOGIN', 'LOGOUT') AND a.createdAt >= :since")
    long countLoginLogoutSince(@Param("userId") Integer userId, @Param("since") LocalDateTime since);

    /**
     * Đếm số lần bị cắm cờ SPAM_WARNING trong 24 giờ qua (để quyết định mức phạt).
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.user.id = :userId AND a.action = 'SPAM_WARNING' AND a.createdAt >= :since")
    long countSpamWarningsSince(@Param("userId") Integer userId, @Param("since") LocalDateTime since);

    /**
     * Tìm thời điểm thực hiện thao tác hợp lệ gần nhất (Không phải Login/Logout/Spam)
     * Dùng làm mốc Reset bộ đếm SPAM (Lấy công chuộc tội).
     */
    @Query("SELECT MAX(a.createdAt) FROM AuditLog a WHERE a.user.id = :userId AND a.action NOT IN ('LOGIN', 'LOGOUT', 'SPAM_WARNING')")
    LocalDateTime findLastLegitimateActionTime(@Param("userId") Integer userId);

    /**
     * Tìm kiếm log nâng cao với nhiều điều kiện (Cho bộ lọc giao diện).
     * Dùng native PostgreSQL query để tránh lỗi lower(bytea) của Hibernate JPQL.
     */
    @Query(value = "SELECT * FROM audit_logs al " +
           "WHERE al.branch_id = :branchId " +
           "AND (:userId IS NULL OR al.user_id = :userId) " +
           "AND (CAST(:action AS TEXT) IS NULL OR al.action = :action) " +
           "AND (CAST(:from AS TIMESTAMP) IS NULL OR al.created_at >= :from) " +
           "AND (CAST(:to AS TIMESTAMP) IS NULL OR al.created_at <= :to) " +
           "AND (CAST(:keyword AS TEXT) IS NULL " +
                "OR CAST(al.details AS TEXT) ILIKE '%' || :keyword || '%' " +
                "OR al.entity_name ILIKE '%' || :keyword || '%' " +
                "OR al.entity_id ILIKE '%' || :keyword || '%') " +
           "ORDER BY al.created_at DESC",
           countQuery = "SELECT count(*) FROM audit_logs al " +
           "WHERE al.branch_id = :branchId " +
           "AND (:userId IS NULL OR al.user_id = :userId) " +
           "AND (CAST(:action AS TEXT) IS NULL OR al.action = :action) " +
           "AND (CAST(:from AS TIMESTAMP) IS NULL OR al.created_at >= :from) " +
           "AND (CAST(:to AS TIMESTAMP) IS NULL OR al.created_at <= :to) " +
           "AND (CAST(:keyword AS TEXT) IS NULL " +
                "OR CAST(al.details AS TEXT) ILIKE '%' || :keyword || '%' " +
                "OR al.entity_name ILIKE '%' || :keyword || '%' " +
                "OR al.entity_id ILIKE '%' || :keyword || '%')",
           nativeQuery = true)
    org.springframework.data.domain.Page<AuditLog> searchLogs(
            @Param("branchId") Integer branchId,
            @Param("userId") Integer userId,
            @Param("action") String action,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * Tự động dọn dẹp các bản ghi quá hạn
     */
    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
