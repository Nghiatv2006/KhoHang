package com.example.Hehe.service;

import com.example.Hehe.model.User;

public interface ReportService {
    byte[] exportInventoryToExcel(Integer targetBranchId, User currentUser);
    
    byte[] exportCustomersToExcel(User currentUser);
    
    java.util.Map<String, Object> getInventoryAgeAnalysis(User currentUser);
    java.util.List<java.util.Map<String, Object>> getStocktakeDiscrepancyHistory(User currentUser);
    java.util.Map<String, Object> getDebtAgingAnalysis(User currentUser);
    byte[] exportRevenueReport(User currentUser, Integer branchId, java.time.LocalDate startDate, java.time.LocalDate endDate, String period);
    
    java.util.Map<Integer, java.util.List<java.math.BigDecimal>> getBranchSalesTrend30Days();

    java.util.Map<String, Object> getRevenueSummary(User currentUser, Integer branchId);
}
