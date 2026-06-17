package com.example.Hehe.dto;

import com.example.Hehe.model.Receipt;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ReceiptResponse {
    private Integer id;
    private String code;
    private String type;
    private String status;
    private String paymentStatus;
    private Integer sourceBranchId;
    private String sourceBranchName;
    private Integer destBranchId;
    private String destBranchName;
    private Integer createdById;
    private String createdByName;
    private Integer customerId;
    private String description;
    private LocalDateTime createdAt;
    private List<ReceiptDetailResponse> details;

    public ReceiptResponse(Receipt r) {
        this.id = r.getId();
        this.code = r.getCode();
        this.type = r.getType().name();
        this.status = r.getStatus().name();
        this.paymentStatus = r.getPaymentStatus();
        
        if (r.getSourceBranch() != null) {
            this.sourceBranchId = r.getSourceBranch().getId();
            this.sourceBranchName = r.getSourceBranch().getName();
        }
        
        if (r.getDestBranch() != null) {
            this.destBranchId = r.getDestBranch().getId();
            this.destBranchName = r.getDestBranch().getName();
        }

        if (r.getCreatedBy() != null) {
            this.createdById = r.getCreatedBy().getId();
            this.createdByName = r.getCreatedBy().getFullName();
        }

        this.customerId = r.getCustomerId();
        this.description = r.getDescription();
        this.createdAt = r.getCreatedAt();
        
        if (r.getDetails() != null) {
            this.details = r.getDetails().stream().map(ReceiptDetailResponse::new).collect(Collectors.toList());
        }
    }

    public Integer getId() { return id; }
    public String getCode() { return code; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getPaymentStatus() { return paymentStatus; }
    public Integer getSourceBranchId() { return sourceBranchId; }
    public String getSourceBranchName() { return sourceBranchName; }
    public Integer getDestBranchId() { return destBranchId; }
    public String getDestBranchName() { return destBranchName; }
    public Integer getCreatedById() { return createdById; }
    public String getCreatedByName() { return createdByName; }
    public Integer getCustomerId() { return customerId; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<ReceiptDetailResponse> getDetails() { return details; }
}
