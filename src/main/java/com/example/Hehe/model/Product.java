package com.example.Hehe.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity đại diện cho bảng products trong database.
 * Lưu trữ thông tin về sản phẩm bao gồm liên kết tới Category, ngày sản xuất và hạn sử dụng.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Mã sản phẩm, ánh xạ vào cột 'code' trong DB
    @Column(name = "code", nullable = false, unique = true)
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Liên kết ManyToOne với Category (Nhiều sản phẩm thuộc 1 danh mục)
    // FetchType.LAZY để tối ưu hiệu suất, chỉ query danh mục khi cần thiết.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "has_expiry", nullable = false)
    private Boolean hasExpiry;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // Ngày sản xuất - Không bắt buộc
    @Column(name = "mfg_date")
    private LocalDate manufacturingDate;

    // Hạn sử dụng - Không bắt buộc
    @Column(name = "exp_date")
    private LocalDate expirationDate;

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Boolean getHasExpiry() {
        return hasExpiry;
    }

    public void setHasExpiry(Boolean hasExpiry) {
        this.hasExpiry = hasExpiry;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
