package com.example.Hehe.dto;

import com.example.Hehe.model.Inventory;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class InventoryResponse {
    private Integer id;
    private Integer branchId;
    private String branchName;
    private Integer productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private LocalDate manufacturingDate;
    private LocalDate expirationDate;
    private LocalDateTime lastUpdated;
    private String batchCode;
    private Boolean hasExpiry;
    private Integer expiryWarningDays;

    public InventoryResponse(Inventory inventory) {
        this.id = inventory.getId();
        this.quantity = inventory.getQuantity();
        this.manufacturingDate = inventory.getManufacturingDate();
        this.expirationDate = inventory.getExpirationDate();
        this.lastUpdated = inventory.getLastUpdated();
        this.batchCode = inventory.getBatchCode();
        this.hasExpiry = inventory.getHasExpiry();
        this.expiryWarningDays = inventory.getExpiryWarningDays();

        if (inventory.getBranch() != null) {
            this.branchId = inventory.getBranch().getId();
            this.branchName = inventory.getBranch().getName();
        }
        
        if (inventory.getProduct() != null) {
            this.productId = inventory.getProduct().getId();
            this.productName = inventory.getProduct().getName();
            this.productSku = inventory.getProduct().getSku();
        }
    }

    public Integer getId() { return id; }
    public Integer getBranchId() { return branchId; }
    public String getBranchName() { return branchName; }
    public Integer getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductSku() { return productSku; }
    public Integer getQuantity() { return quantity; }
    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public String getBatchCode() { return batchCode; }
    public Boolean getHasExpiry() { return hasExpiry; }
    public Integer getExpiryWarningDays() { return expiryWarningDays; }
}
