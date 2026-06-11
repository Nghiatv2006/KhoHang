package com.example.Hehe.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO nhận dữ liệu từ client khi thêm mới hoặc cập nhật Product.
 * Không chứa ID và SKU vì ID tự tăng và SKU tự động sinh bởi hệ thống.
 */
public class ProductSaveRequest {

    private String name;
    private BigDecimal price;
    private Integer quantity;
    private String description;
    
    // ID của danh mục mà người dùng chọn
    private Integer categoryId;

    private LocalDate manufacturingDate;
    private LocalDate expirationDate;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
