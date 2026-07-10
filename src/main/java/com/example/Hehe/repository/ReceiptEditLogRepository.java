package com.example.Hehe.repository;

import com.example.Hehe.model.ReceiptEditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptEditLogRepository extends JpaRepository<ReceiptEditLog, Long> {

    /** Toàn bộ lịch sử sửa của một phiếu, mới nhất trước */
    List<ReceiptEditLog> findByReceiptIdOrderByCreatedAtDesc(Integer receiptId);

    /**
     * Tìm log MANAGER_TO_STAFF chưa được Staff xác nhận.
     * Dùng để kiểm tra có pending acknowledge không trước khi đổi trạng thái.
     */
    Optional<ReceiptEditLog> findTopByReceiptIdAndDirectionAndAcknowledgedAtIsNull(
            Integer receiptId, String direction);
}
