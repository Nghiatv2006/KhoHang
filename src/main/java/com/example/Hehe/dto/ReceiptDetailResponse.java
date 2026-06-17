package com.example.Hehe.dto;

import com.example.Hehe.model.ReceiptDetail;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReceiptDetailResponse {
    private Integer id;
    private Integer productId;
    private String productName;
    private String productSku;
    private LocalDate manufacturingDate;
    private LocalDate expirationDate;
    private Integer quantity;
    private BigDecimal price;

    public ReceiptDetailResponse(ReceiptDetail d) {
        this.id = d.getId();
        this.manufacturingDate = d.getManufacturingDate();
        this.expirationDate = d.getExpirationDate();
        this.quantity = d.getQuantity();
        this.price = d.getPrice();

        if (d.getProduct() != null) {
            this.productId = d.getProduct().getId();
            this.productName = d.getProduct().getName();
            this.productSku = d.getProduct().getSku();
        }
    }

    public Integer getId() { return id; }
    public Integer getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductSku() { return productSku; }
    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
}
