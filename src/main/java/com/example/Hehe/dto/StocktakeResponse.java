package com.example.Hehe.dto;

import com.example.Hehe.model.Stocktake;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class StocktakeResponse {
    private Integer id;
    private String code;
    private Integer branchId;
    private String branchName;
    private Integer createdById;
    private String createdByName;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private List<StocktakeDetailResponse> details;

    public StocktakeResponse(Stocktake s) {
        this.id = s.getId();
        this.code = s.getCode();
        this.status = s.getStatus().name();
        this.notes = s.getNotes();
        this.createdAt = s.getCreatedAt();
        if (s.getBranch() != null) {
            this.branchId = s.getBranch().getId();
            this.branchName = s.getBranch().getName();
        }
        if (s.getCreatedBy() != null) {
            this.createdById = s.getCreatedBy().getId();
            this.createdByName = s.getCreatedBy().getFullName();
        }
        if (s.getDetails() != null) {
            this.details = s.getDetails().stream().map(StocktakeDetailResponse::new).collect(Collectors.toList());
        }
    }

    public Integer getId() { return id; }
    public String getCode() { return code; }
    public Integer getBranchId() { return branchId; }
    public String getBranchName() { return branchName; }
    public Integer getCreatedById() { return createdById; }
    public String getCreatedByName() { return createdByName; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<StocktakeDetailResponse> getDetails() { return details; }
}
