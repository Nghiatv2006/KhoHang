package com.example.Hehe.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "stocktake_details")
public class StocktakeDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stocktake_id", nullable = false)
    private Stocktake stocktake;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "mfg_date", nullable = false)
    private LocalDate manufacturingDate = LocalDate.of(1970, 1, 1);

    @Column(name = "exp_date", nullable = false)
    private LocalDate expirationDate = LocalDate.of(1970, 1, 1);

    @Column(name = "batch_code", nullable = false, length = 100)
    private String batchCode = "DEFAULT_BATCH";

    @Column(name = "expected_quantity", nullable = false)
    private Integer expectedQuantity = 0;

    @Column(name = "actual_quantity", nullable = false)
    private Integer actualQuantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_receipt_id")
    private Receipt adjustmentReceipt;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Stocktake getStocktake() {
        return stocktake;
    }

    public void setStocktake(Stocktake stocktake) {
        this.stocktake = stocktake;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public void setManufacturingDate(LocalDate manufacturingDate) {
        this.manufacturingDate = manufacturingDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public Integer getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Integer expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public Integer getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(Integer actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public Receipt getAdjustmentReceipt() {
        return adjustmentReceipt;
    }

    public void setAdjustmentReceipt(Receipt adjustmentReceipt) {
        this.adjustmentReceipt = adjustmentReceipt;
    }
}
