package com.example.Hehe.dto;

public class BranchResponse {
    private Integer id;
    private String name;
    private String address;
    private Integer lowStockThreshold;
    private Boolean isHead;
    private String taxCode;
    private String managerName;

    public BranchResponse() {}

    public BranchResponse(Integer id, String name, String address, Integer lowStockThreshold, Boolean isHead, String taxCode, String managerName) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.lowStockThreshold = lowStockThreshold;
        this.isHead = isHead;
        this.taxCode = taxCode;
        this.managerName = managerName;
    }

    // Getters and Setters
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

    public Boolean getIsHead() {
        return isHead;
    }

    public void setIsHead(Boolean isHead) {
        this.isHead = isHead;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }
}
