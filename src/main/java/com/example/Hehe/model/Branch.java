package com.example.Hehe.model;

import jakarta.persistence.*;

@Entity
@Table(name = "branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 5;

    @Column(name = "is_head", nullable = false)
    private Boolean isHead = false;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    // Getters and Setters
    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }
    public Boolean getIsHead() {
        return isHead;
    }

    public void setIsHead(Boolean isHead) {
        this.isHead = isHead;
    }
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Boolean getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }
}
