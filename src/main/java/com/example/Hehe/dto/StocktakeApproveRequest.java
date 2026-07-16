package com.example.Hehe.dto;

public class StocktakeApproveRequest {
    private String reason;
    private Integer responsibleUserId;
    private String responsiblePersonName;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getResponsibleUserId() {
        return responsibleUserId;
    }

    public void setResponsibleUserId(Integer responsibleUserId) {
        this.responsibleUserId = responsibleUserId;
    }

    public String getResponsiblePersonName() {
        return responsiblePersonName;
    }

    public void setResponsiblePersonName(String responsiblePersonName) {
        this.responsiblePersonName = responsiblePersonName;
    }
}
