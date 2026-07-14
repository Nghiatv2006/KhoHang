package com.example.Hehe.dto;

import com.example.Hehe.model.ReceiptDetail;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReceiptDetailResponse {
    private Integer id;
    private Integer productId;
    private String productName;
    private String productSku;
    private String productCategory;
    private LocalDate manufacturingDate;
    private LocalDate expirationDate;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal importPrice;
    private String batchCode;
    private Integer receivedQuantity;
    private String shortfallReason;

    public ReceiptDetailResponse(ReceiptDetail d) {
        this.id = d.getId();
        this.manufacturingDate = d.getManufacturingDate();
        this.expirationDate = d.getExpirationDate();
        this.price = d.getPrice();
        this.quantity = d.getQuantity();
        this.batchCode = d.getBatchCode();
        this.receivedQuantity = d.getReceivedQuantity();
        this.shortfallReason = d.getShortfallReason();

        if (d.getProduct() != null) {
            this.productId = d.getProduct().getId();
            this.productName = d.getProduct().getName();
            this.productSku = d.getProduct().getSku();
            this.importPrice = d.getProduct().getImportPrice() != null
                    ? d.getProduct().getImportPrice() : java.math.BigDecimal.ZERO;
            if (d.getProduct().getCategory() != null) {
                this.productCategory = d.getProduct().getCategory().getName();
            }
        }
    }

    public Integer getId() { return id; }
    public Integer getProductId() { return productId; }
    public String getBatchCode() { return batchCode; }
    public Integer getReceivedQuantity() { return receivedQuantity; }
    public String getShortfallReason() { return shortfallReason; }
    public String getProductCategory() { return productCategory; }
    public String getProductSku() { return productSku; }
    public String getProductName() { return productName; }
    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getImportPrice() { return importPrice; }
}
