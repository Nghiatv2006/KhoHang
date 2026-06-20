package com.example.Hehe.dto;

import java.time.LocalDate;

public class InventorySaveRequest {
    private Integer productId;
    private Integer branchId;
    private String batchCode;
    private LocalDate manufacturingDate;
    private LocalDate expirationDate;
    private Integer quantity;
    private Boolean hasExpiry;
    private Integer expiryWarningDays;

    // Getters and Setters
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }

    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Boolean getHasExpiry() { return hasExpiry; }
    public void setHasExpiry(Boolean hasExpiry) { this.hasExpiry = hasExpiry; }

    public Integer getExpiryWarningDays() { return expiryWarningDays; }
    public void setExpiryWarningDays(Integer expiryWarningDays) { this.expiryWarningDays = expiryWarningDays; }
}
