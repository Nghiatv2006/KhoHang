package com.example.Hehe.dto;

import com.example.Hehe.model.AuditLog;
import java.time.LocalDateTime;

/**
 * DTO trả về một dòng Nhật ký hoạt động cho Frontend.
 */
public class AuditLogResponse {
    private Long id;
    private Integer userId;
    private String userFullName;
    private String username;
    private Integer branchId;
    private String action;
    private String entityName;
    private String entityId;
    private String details;
    private LocalDateTime createdAt;
    private Boolean isWarning;

    public AuditLogResponse(AuditLog log) {
        this.id = log.getId();
        this.branchId = log.getBranchId();
        this.action = log.getAction();
        this.entityName = log.getEntityName();
        this.entityId = log.getEntityId();
        this.details = log.getDetails();
        this.createdAt = log.getCreatedAt();
        this.isWarning = log.getIsWarning();

        if (log.getUser() != null) {
            this.userId = log.getUser().getId();
            this.userFullName = log.getUser().getFullName();
            this.username = log.getUser().getUsername();
        } else {
            this.userFullName = "[Người dùng đã bị xóa]";
            this.username = "N/A";
        }
    }

    // Getters
    public Long getId() { return id; }
    public Integer getUserId() { return userId; }
    public String getUserFullName() { return userFullName; }
    public String getUsername() { return username; }
    public Integer getBranchId() { return branchId; }
    public String getAction() { return action; }
    public String getEntityName() { return entityName; }
    public String getEntityId() { return entityId; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Boolean getIsWarning() { return isWarning; }
}
