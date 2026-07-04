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
    private String createdByRole;
    private Integer stocktakeById;
    private String stocktakeByName;
    private Integer approvedById;
    private String approvedByName;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private String description;
    private LocalDateTime createdAt;
    private List<ReceiptDetailResponse> details;
    private Boolean hasDeviation;
    private String deviationSummary;
    private String disposalReason;
    private String disposalMethod;
    private String attachmentUrl;

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
            this.createdByRole = r.getCreatedBy().getRole() != null ? r.getCreatedBy().getRole().name() : null;
        }

        if (r.getStocktakeBy() != null) {
            this.stocktakeById = r.getStocktakeBy().getId();
            this.stocktakeByName = r.getStocktakeBy().getFullName();
        }

        if (r.getApprovedBy() != null) {
            this.approvedById = r.getApprovedBy().getId();
            this.approvedByName = r.getApprovedBy().getFullName();
        }

        this.customerId = r.getCustomerId();
        this.customerName = r.getCustomerName();
        this.customerPhone = r.getCustomerPhone();
        this.description = r.getDescription();
        this.disposalReason = r.getDisposalReason();
        this.disposalMethod = r.getDisposalMethod();
        this.attachmentUrl = r.getAttachmentUrl();
        this.createdAt = r.getCreatedAt();
        
        this.hasDeviation = false;
        StringBuilder sb = new StringBuilder();
        if (r.getDetails() != null) {
            this.details = r.getDetails().stream().map(ReceiptDetailResponse::new).collect(Collectors.toList());
            for (com.example.Hehe.model.ReceiptDetail d : r.getDetails()) {
                if (d.getReceivedQuantity() != null && !d.getReceivedQuantity().equals(d.getQuantity())) {
                    this.hasDeviation = true;
                    int diff = d.getReceivedQuantity() - d.getQuantity();
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(diff > 0 ? "+" : "").append(diff).append(" ").append(d.getProduct() != null ? d.getProduct().getName() : "SP");
                    if (d.getShortfallReason() != null && !d.getShortfallReason().trim().isEmpty()) {
                        sb.append(" (").append(d.getShortfallReason().trim()).append(")");
                    }
                }
            }
        }
        this.deviationSummary = sb.length() > 0 ? sb.toString() : null;
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
    public String getCreatedByRole() { return createdByRole; }
    public Integer getStocktakeById() { return stocktakeById; }
    public String getStocktakeByName() { return stocktakeByName; }
    public Integer getApprovedById() { return approvedById; }
    public String getApprovedByName() { return approvedByName; }
    public Integer getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<ReceiptDetailResponse> getDetails() { return details; }
    public Boolean getHasDeviation() { return hasDeviation; }
    public String getDeviationSummary() { return deviationSummary; }
    public String getDisposalReason() { return disposalReason; }
    public String getDisposalMethod() { return disposalMethod; }
    public String getAttachmentUrl() { return attachmentUrl; }
}
