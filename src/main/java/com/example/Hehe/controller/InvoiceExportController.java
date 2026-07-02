package com.example.Hehe.controller;

import com.example.Hehe.model.User;
import com.example.Hehe.service.InvoiceExcelService;
import com.example.Hehe.service.InvoicePdfService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceExportController {

    private final InvoicePdfService  invoicePdfService;
    private final InvoiceExcelService invoiceExcelService;

    public InvoiceExportController(InvoicePdfService invoicePdfService,
                                   InvoiceExcelService invoiceExcelService) {
        this.invoicePdfService   = invoicePdfService;
        this.invoiceExcelService = invoiceExcelService;
    }

    /**
     * Xuất PDF hóa đơn in nhiệt 80mm cho 1 phiếu.
     * Quyền: ADMIN, MANAGER, STAFF (staff chỉ xem được phiếu của chi nhánh mình)
     *
     * GET /api/invoices/{id}/export-pdf
     */
    @GetMapping("/{id}/export-pdf")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {

        byte[] pdfBytes = invoicePdfService.exportReceiptPdf(id, currentUser);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Hoa_Don_" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    /**
     * Xuất Excel danh sách hóa đơn theo bộ lọc thời gian.
     * Quyền: ADMIN (toàn hệ thống), MANAGER (chi nhánh mình). STAFF bị chặn.
     *
     * GET /api/invoices/export-excel?startDate=2026-06-01&endDate=2026-06-30
     */
    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer branchId,
            @AuthenticationPrincipal User currentUser) {

        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay()         : null;
        LocalDateTime endDt   = endDate   != null ? endDate.atTime(LocalTime.MAX)    : null;

        byte[] excelBytes = invoiceExcelService.exportInvoiceList(currentUser, startDt, endDt, branchId);

        // Tên file chứa khoảng thời gian để kế toán dễ nhận biết
        String fileName = buildFileName(startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    private String buildFileName(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "Danh_Sach_Hoa_Don_Ban_Hang.xlsx";
        }
        String from = startDate != null ? startDate.toString().replace("-", "") : "TuDau";
        String to   = endDate   != null ? endDate.toString().replace("-", "")   : "DenNay";
        return "Danh_Sach_Hoa_Don_" + from + "_" + to + ".xlsx";
    }
}
