package com.example.Hehe.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bảng lưu lịch sử chỉnh sửa phiếu (receipt_edit_logs).
 * Ghi lại mỗi lần Staff hoặc Manager chỉnh sửa số lượng / ghi chú của phiếu.
 */
@Entity
@Table(name = "receipt_edit_logs")
public class ReceiptEditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Phiếu được chỉnh sửa */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    /** Người thực hiện chỉnh sửa */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editor_id")
    private User editor;

    /** Vai trò của người sửa: STAFF | MANAGER */
    @Column(name = "editor_role", length = 20)
    private String editorRole;

    /**
     * Chiều chỉnh sửa:
     *   STAFF_EDIT        — Staff tự sửa (Manager được xem)
     *   MANAGER_TO_STAFF  — Manager sửa + gửi xuống Staff xác nhận
     *   MANAGER_TO_ADMIN  — Manager sửa khi phiếu ở PENDING_ADMIN để Admin xem
     */
    @Column(name = "direction", length = 30, nullable = false)
    private String direction;

    /** Lý do chỉnh sửa (bắt buộc khi tạo log) */
    @Column(name = "edit_reason", columnDefinition = "TEXT", nullable = false)
    private String editReason;

    /** Mô tả nội dung thay đổi, VD: "Sữa Vinamilk: 10→15, Sữa TH: 5→3" */
    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes;

    /** Người xác nhận (Staff xác nhận log MANAGER_TO_STAFF) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private User acknowledgedBy;

    /** Thời điểm Staff xác nhận, null = chưa xác nhận */
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ─── Getters / Setters ──────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Receipt getReceipt() { return receipt; }
    public void setReceipt(Receipt receipt) { this.receipt = receipt; }

    public User getEditor() { return editor; }
    public void setEditor(User editor) { this.editor = editor; }

    public String getEditorRole() { return editorRole; }
    public void setEditorRole(String editorRole) { this.editorRole = editorRole; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getEditReason() { return editReason; }
    public void setEditReason(String editReason) { this.editReason = editReason; }

    public String getChanges() { return changes; }
    public void setChanges(String changes) { this.changes = changes; }

    public User getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(User acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }

    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
