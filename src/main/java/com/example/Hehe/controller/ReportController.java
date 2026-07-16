package com.example.Hehe.controller;

import com.example.Hehe.model.User;
import com.example.Hehe.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/inventory/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> exportInventoryToExcel(
            @RequestParam Integer branchId,
            @AuthenticationPrincipal User currentUser) {
        
        byte[] excelBytes = reportService.exportInventoryToExcel(branchId, currentUser);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "Bao_Cao_Ton_Kho_" + branchId + ".xlsx");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/customers/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> exportCustomersExcel(
            @AuthenticationPrincipal User currentUser) {
        
        byte[] excelBytes = reportService.exportCustomersToExcel(currentUser);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "Bao_Cao_Cong_No_Khach_Hang.xlsx");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/dashboard/inventory-age")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<java.util.Map<String, Object>> getInventoryAgeAnalysis(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getInventoryAgeAnalysis(currentUser));
    }

    @GetMapping("/dashboard/stocktake-discrepancy")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getStocktakeDiscrepancyHistory(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getStocktakeDiscrepancyHistory(currentUser));
    }

    @GetMapping("/dashboard/debt-aging")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<java.util.Map<String, Object>> getDebtAgingAnalysis(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getDebtAgingAnalysis(currentUser));
    }

    @GetMapping("/revenue/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> exportRevenueExcel(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer branchId,
            @AuthenticationPrincipal User currentUser) {

        java.time.LocalDate startDt = null;
        java.time.LocalDate endDt = null;
        if (startDate != null && !startDate.isEmpty()) {
            startDt = java.time.LocalDate.parse(startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            endDt = java.time.LocalDate.parse(endDate);
        }

        byte[] excelBytes = reportService.exportRevenueReport(currentUser, branchId, startDt, endDt, period);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "Bao_Cao_Doanh_Thu.xlsx");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/dashboard/branch-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<java.util.Map<Integer, java.util.List<java.math.BigDecimal>>> getBranchSalesTrend30Days(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getBranchSalesTrend30Days(currentUser));
    }

    @GetMapping("/revenue/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<java.util.Map<String, Object>> getRevenueSummary(
            @RequestParam(required = false) Integer branchId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getRevenueSummary(currentUser, branchId));
    }
}

