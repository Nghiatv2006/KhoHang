package com.example.Hehe.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity ghi lại Nhật ký hoạt động của toàn bộ người dùng trong hệ thống.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Người thực hiện hành động. SET NULL nếu user bị xóa để không mất log lịch sử.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /**
     * Chi nhánh mà hành động này thuộc về.
     * ADMIN -> branchId = 1. MANAGER/STAFF -> branchId của họ.
     */
    @Column(name = "branch_id")
    private Integer branchId;

    /**
     * Loại hành động: LOGIN, LOGOUT, CREATE, UPDATE, DELETE, APPROVE, CANCEL, SPAM_WARNING, LOCK_ACCOUNT
     */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /**
     * Tên thực thể bị tác động: products, receipts, users, categories, ...
     */
    @Column(name = "entity_name", length = 100)
    private String entityName;

    /**
     * ID của thực thể bị tác động (để sau này có thể tra cứu ngược lại)
     */
    @Column(name = "entity_id", length = 100)
    private String entityId;

    /**
     * Mô tả chi tiết bằng ngôn ngữ tự nhiên (Tiếng Việt)
     * Ví dụ: "Cập nhật giá bán iPhone 15 từ 20,000,000đ lên 21,000,000đ"
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /**
     * Thời điểm thực hiện hành động, tự động gán khi tạo.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Đánh dấu log này là Cảnh báo/Nguy hiểm (hiển thị màu đỏ trên giao diện).
     */
    @Column(name = "is_warning", nullable = false)
    private Boolean isWarning = false;

    // ─── Constructors ───────────────────────────────────────────────────────

    public AuditLog() {}

    public AuditLog(User user, Integer branchId, String action, String entityName, String entityId, String details) {
        this.user = user;
        this.branchId = branchId;
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.details = details;
        this.isWarning = false;
        this.createdAt = LocalDateTime.now();
    }

    public AuditLog(User user, Integer branchId, String action, String entityName, String entityId, String details, Boolean isWarning) {
        this(user, branchId, action, entityName, entityId, details);
        this.isWarning = isWarning;
    }

    // ─── Getters and Setters ─────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getIsWarning() { return isWarning; }
    public void setIsWarning(Boolean isWarning) { this.isWarning = isWarning; }
}
