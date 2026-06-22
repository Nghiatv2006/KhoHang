package com.example.Hehe.dto;

import com.example.Hehe.model.ReceiptType;
import java.util.List;

public class ReceiptSaveRequest {
    private ReceiptType type;
    private String paymentStatus;
    private Integer sourceBranchId;
    private Integer destBranchId;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private String description;
    private List<ReceiptDetailSaveRequest> details;

    public ReceiptType getType() { return type; }
    public void setType(ReceiptType type) { this.type = type; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Integer getSourceBranchId() { return sourceBranchId; }
    public void setSourceBranchId(Integer sourceBranchId) { this.sourceBranchId = sourceBranchId; }

    public Integer getDestBranchId() { return destBranchId; }
    public void setDestBranchId(Integer destBranchId) { this.destBranchId = destBranchId; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<ReceiptDetailSaveRequest> getDetails() { return details; }
    public void setDetails(List<ReceiptDetailSaveRequest> details) { this.details = details; }
}
