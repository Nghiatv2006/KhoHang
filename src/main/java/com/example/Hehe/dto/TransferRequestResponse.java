package com.example.Hehe.dto;

import java.time.LocalDateTime;

public class TransferRequestResponse {
    private Integer id;
    private Integer staffId;
    private String staffName;
    private Integer fromBranchId;
    private String fromBranchName;
    private Integer toBranchId;
    private String toBranchName;
    private Integer createdById;
    private String createdByName;
    private String status;
    private LocalDateTime createdAt;
    private Integer approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;

    public TransferRequestResponse() {}

    public TransferRequestResponse(Integer id, Integer staffId, String staffName, Integer fromBranchId, String fromBranchName, Integer toBranchId, String toBranchName, Integer createdById, String createdByName, String status, LocalDateTime createdAt, Integer approvedById, String approvedByName, LocalDateTime approvedAt) {
        this.id = id;
        this.staffId = staffId;
        this.staffName = staffName;
        this.fromBranchId = fromBranchId;
        this.fromBranchName = fromBranchName;
        this.toBranchId = toBranchId;
        this.toBranchName = toBranchName;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.status = status;
        this.createdAt = createdAt;
        this.approvedById = approvedById;
        this.approvedByName = approvedByName;
        this.approvedAt = approvedAt;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public Integer getFromBranchId() {
        return fromBranchId;
    }

    public void setFromBranchId(Integer fromBranchId) {
        this.fromBranchId = fromBranchId;
    }

    public String getFromBranchName() {
        return fromBranchName;
    }

    public void setFromBranchName(String fromBranchName) {
        this.fromBranchName = fromBranchName;
    }

    public Integer getToBranchId() {
        return toBranchId;
    }

    public void setToBranchId(Integer toBranchId) {
        this.toBranchId = toBranchId;
    }

    public String getToBranchName() {
        return toBranchName;
    }

    public void setToBranchName(String toBranchName) {
        this.toBranchName = toBranchName;
    }

    public Integer getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Integer createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getApprovedById() {
        return approvedById;
    }

    public void setApprovedById(Integer approvedById) {
        this.approvedById = approvedById;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}
