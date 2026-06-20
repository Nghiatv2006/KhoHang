package com.example.Hehe.dto;

import java.util.List;

public class StocktakeSaveRequest {
    private String notes;
    private List<StocktakeDetailSaveRequest> details;

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<StocktakeDetailSaveRequest> getDetails() { return details; }
    public void setDetails(List<StocktakeDetailSaveRequest> details) { this.details = details; }
}
