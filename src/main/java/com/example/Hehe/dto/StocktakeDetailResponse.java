package com.example.Hehe.dto;

import com.example.Hehe.model.StocktakeDetail;
import java.time.LocalDate;

public class StocktakeDetailResponse {
    private Integer id;
    private Integer productId;
    private String productName;
    private String productSku;
    private String productUnit;
    private LocalDate manufacturingDate;
    private LocalDate expirationDate;
    private String batchCode;
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private Integer adjustmentReceiptId;
    private String adjustmentReceiptCode;

    public StocktakeDetailResponse(StocktakeDetail d) {
        this.id = d.getId();
        this.manufacturingDate = d.getManufacturingDate();
        this.expirationDate = d.getExpirationDate();
        this.batchCode = d.getBatchCode();
        this.expectedQuantity = d.getExpectedQuantity();
        this.actualQuantity = d.getActualQuantity();
        if (d.getProduct() != null) {
            this.productId = d.getProduct().getId();
            this.productName = d.getProduct().getName();
            this.productSku = d.getProduct().getSku();
            this.productUnit = d.getProduct().getUnit();
        }
        if (d.getAdjustmentReceipt() != null) {
            this.adjustmentReceiptId = d.getAdjustmentReceipt().getId();
            this.adjustmentReceiptCode = d.getAdjustmentReceipt().getCode();
        }
    }

    public Integer getId() { return id; }
    public Integer getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductSku() { return productSku; }
    public String getProductUnit() { return productUnit; }
    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public String getBatchCode() { return batchCode; }
    public Integer getExpectedQuantity() { return expectedQuantity; }
    public Integer getActualQuantity() { return actualQuantity; }
    public Integer getAdjustmentReceiptId() { return adjustmentReceiptId; }
    public String getAdjustmentReceiptCode() { return adjustmentReceiptCode; }
}
