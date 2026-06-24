package com.example.Hehe.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper bean tách biệt để thực hiện LOCK / UNLOCK chi nhánh trong
 * một transaction ĐỘC LẬP (REQUIRES_NEW).
 *
 * Lý do cần tách ra bean riêng:
 *   Spring AOP chỉ áp dụng @Transactional qua proxy. Nếu gọi method trong
 *   cùng một class (self-invocation), annotation bị bỏ qua → không có
 *   transaction mới. Bằng cách tách sang bean riêng, Spring sẽ tạo proxy
 *   đúng cách, đảm bảo REQUIRES_NEW hoạt động chính xác.
 *
 * Kết quả:
 *   - lock()   → UPDATE is_locked=true  COMMIT ngay lập tức
 *                 → BranchLockInterceptor thấy trạng thái mới trong DB
 *   - unlock() → UPDATE is_locked=false COMMIT ngay lập tức
 *                 → Chạy trong finally, đảm bảo unlock kể cả khi có lỗi
 */
@Component
public class BranchLockHelper {

    private final JdbcTemplate jdbcTemplate;

    public BranchLockHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Khóa chi nhánh ngay lập tức, commit trong transaction riêng.
     * Mọi request API vào chi nhánh này sẽ nhận HTTP 423 sau lời gọi này.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lock(Integer branchId) {
        jdbcTemplate.update("UPDATE branches SET is_locked = true WHERE id = ?", branchId);
    }

    /**
     * Mở khóa chi nhánh ngay lập tức, commit trong transaction riêng.
     * Nên luôn gọi trong khối finally để đảm bảo unlock kể cả khi có exception.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlock(Integer branchId) {
        jdbcTemplate.update("UPDATE branches SET is_locked = false WHERE id = ?", branchId);
    }
}
