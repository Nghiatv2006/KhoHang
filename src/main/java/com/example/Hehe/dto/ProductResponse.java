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
    private BigDecimal importPrice;
    private BigDecimal price;

    private String imageUrl;
    private String unit;
    
    private Integer categoryId;
    private String categoryName;

    private LocalDate manufacturingDate;
    private LocalDate expirationDate;

    private Boolean hasExpiry;

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.sku = product.getSku();
        this.name = product.getName();
        this.importPrice = product.getImportPrice();
        this.price = product.getPrice();
        this.unit = product.getUnit();
        this.hasExpiry = product.getHasExpiry();

        this.imageUrl = product.getImageUrl();
        
        // Trích xuất ID và Tên của Category thay vì trả toàn bộ object Category
        if (product.getCategory() != null) {
            this.categoryId = product.getCategory().getId();
            this.categoryName = product.getCategory().getName();
        }

        this.manufacturingDate = product.getManufacturingDate();
        this.expirationDate = product.getExpirationDate();
        this.hasExpiry = product.getHasExpiry();
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

    public BigDecimal getImportPrice() {
        return importPrice;
    }

    public BigDecimal getPrice() {
        return price;
    }


    public String getImageUrl() {
        return imageUrl;
    }

    public String getUnit() {
        return unit;
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


    public Boolean getHasExpiry() {
        return hasExpiry;
    }
}
