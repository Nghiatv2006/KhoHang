package com.example.Hehe.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO nhận dữ liệu từ client khi thêm mới hoặc cập nhật Product.
 * Không chứa ID và SKU vì ID tự tăng và SKU tự động sinh bởi hệ thống.
 */
public class ProductSaveRequest {

    private String name;
    private BigDecimal importPrice;
    private BigDecimal price;

    private String imageUrl;
    private String unit;
    
    // ID của danh mục mà người dùng chọn
    private Integer categoryId;

    private LocalDate manufacturingDate;
    private LocalDate expirationDate;
    private Boolean hasExpiry;
    
    // Removed forceCreate

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getImportPrice() {
        return importPrice;
    }

    public void setImportPrice(BigDecimal importPrice) {
        this.importPrice = importPrice;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
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

    public Boolean getHasExpiry() {
        return hasExpiry;
    }

    public void setHasExpiry(Boolean hasExpiry) {
        this.hasExpiry = hasExpiry;
    }


}
