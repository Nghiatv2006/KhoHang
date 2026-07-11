package com.example.Hehe.dto;

import com.example.Hehe.model.ReceiptEditLog;
import java.time.LocalDateTime;

public class ReceiptEditLogResponse {
    private Long id;
    private Integer editorId;
    private String editorName;
    private String editorRole;
    private String direction;
    private String editReason;
    private String changes;
    private Integer acknowledgedById;
    private String acknowledgedByName;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime createdAt;

    public ReceiptEditLogResponse(ReceiptEditLog log) {
        this.id = log.getId();
        if (log.getEditor() != null) {
            this.editorId = log.getEditor().getId();
            this.editorName = log.getEditor().getFullName();
        }
        this.editorRole = log.getEditorRole();
        this.direction = log.getDirection();
        this.editReason = log.getEditReason();
        this.changes = log.getChanges();
        if (log.getAcknowledgedBy() != null) {
            this.acknowledgedById = log.getAcknowledgedBy().getId();
            this.acknowledgedByName = log.getAcknowledgedBy().getFullName();
        }
        this.acknowledgedAt = log.getAcknowledgedAt();
        this.createdAt = log.getCreatedAt();
    }

    public Long getId() { return id; }
    public Integer getEditorId() { return editorId; }
    public String getEditorName() { return editorName; }
    public String getEditorRole() { return editorRole; }
    public String getDirection() { return direction; }
    public String getEditReason() { return editReason; }
    public String getChanges() { return changes; }
    public Integer getAcknowledgedById() { return acknowledgedById; }
    public String getAcknowledgedByName() { return acknowledgedByName; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
