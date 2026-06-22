package com.example.Hehe.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventories")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "mfg_date", nullable = false)
    private LocalDate manufacturingDate;

    @Column(name = "exp_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "batch_code", nullable = false, length = 100, columnDefinition = "varchar(100) default 'DEFAULT_BATCH'")
    private String batchCode = "DEFAULT_BATCH";

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "has_expiry", nullable = false, columnDefinition = "boolean default false")
    private Boolean hasExpiry = false;

    @Column(name = "expiry_warning_days", nullable = false, columnDefinition = "int default 30")
    private Integer expiryWarningDays = 30;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public Boolean getHasExpiry() { return hasExpiry; }
    public void setHasExpiry(Boolean hasExpiry) { this.hasExpiry = hasExpiry; }

    public Integer getExpiryWarningDays() { return expiryWarningDays; }
    public void setExpiryWarningDays(Integer expiryWarningDays) { this.expiryWarningDays = expiryWarningDays; }
}
