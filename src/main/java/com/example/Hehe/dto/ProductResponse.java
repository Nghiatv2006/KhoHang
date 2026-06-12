package com.example.Hehe.dto;

import com.example.Hehe.model.Product;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO trả dữ liệu Product về cho client.
 * Tránh trả trực tiếp Entity để không lộ các thông tin không cần thiết
 * và xử lý được lỗi vòng lặp vô hạn (Infinite Recursion) với JSON.
 */
public class ProductResponse {

    private Integer id;
    private String sku;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private String description;
    
    private Integer categoryId;
    private String categoryName;

    private LocalDate manufacturingDate;
    private LocalDate expirationDate;

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.sku = product.getSku();
        this.name = product.getName();
        this.price = product.getPrice();
        this.quantity = product.getQuantity();
        this.description = product.getDescription();
        
        // Trích xuất ID và Tên của Category thay vì trả toàn bộ object Category
        if (product.getCategory() != null) {
            this.categoryId = product.getCategory().getId();
            this.categoryName = product.getCategory().getName();
        }

        this.manufacturingDate = product.getManufacturingDate();
        this.expirationDate = product.getExpirationDate();
    }

    // Getters

    public Integer getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getDescription() {
        return description;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }
}
