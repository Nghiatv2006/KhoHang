package com.example.Hehe.dto;

public class TransferRequestSaveRequest {
    private Integer staffId;
    private Integer toBranchId;

    public TransferRequestSaveRequest() {}

    // Getters and Setters
    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public Integer getToBranchId() {
        return toBranchId;
    }

    public void setToBranchId(Integer toBranchId) {
        this.toBranchId = toBranchId;
    }
}
