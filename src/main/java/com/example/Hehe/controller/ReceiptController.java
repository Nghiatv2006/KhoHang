package com.example.Hehe.controller;

import com.example.Hehe.dto.EditReceiptRequest;
import com.example.Hehe.dto.ReceiptEditLogResponse;
import com.example.Hehe.dto.ReceiptResponse;
import com.example.Hehe.dto.ReceiptSaveRequest;
import com.example.Hehe.model.User;
import com.example.Hehe.service.ReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping
    public ResponseEntity<List<ReceiptResponse>> getAllReceipts(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.getAllReceipts(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponse> getReceiptById(@PathVariable Integer id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.getReceiptById(id, currentUser));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ReceiptResponse>> getReceiptsByCustomer(@PathVariable Integer customerId, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.getReceiptsByCustomer(customerId, currentUser));
    }

    @PostMapping
    public ResponseEntity<ReceiptResponse> createReceipt(@RequestBody ReceiptSaveRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.createReceipt(request, currentUser));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReceiptResponse> cancelReceipt(@PathVariable Integer id, @RequestParam(required = false) String reason, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.cancelReceipt(id, reason, currentUser));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ReceiptResponse> approveReceipt(@PathVariable Integer id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.approveReceipt(id, currentUser));
    }

    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<ReceiptResponse> markPaid(@PathVariable Integer id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.markPaid(id, currentUser));
    }

    @PostMapping("/{id}/confirm-transfer")
    public ResponseEntity<ReceiptResponse> confirmTransfer(@PathVariable Integer id, @RequestBody java.util.Map<String, Object> payload, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(receiptService.confirmTransfer(id, payload, user));
    }

    @PostMapping("/{id}/confirm-stocktake")
    public ResponseEntity<ReceiptResponse> confirmStocktake(@PathVariable Integer id, @RequestBody java.util.Map<String, Object> payload, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(receiptService.confirmStocktake(id, payload, user));
    }

    @PostMapping("/{id}/approve-shortfall")
    public ResponseEntity<ReceiptResponse> approveShortfall(@PathVariable Integer id, @RequestBody java.util.Map<String, Boolean> payload, @AuthenticationPrincipal User user) {
        boolean isApproved = payload.getOrDefault("isApproved", false);
        return ResponseEntity.ok(receiptService.approveShortfall(id, isApproved, user));
    }

    @PostMapping("/{id}/compensate-shortfall")
    public ResponseEntity<ReceiptResponse> compensateShortfall(@PathVariable Integer id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(receiptService.compensateShortfall(id, user));
    }

    @GetMapping("/completed-branch")
    public ResponseEntity<List<ReceiptResponse>> getCompletedBranchReceipts(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.getCompletedBranchReceipts(currentUser));
    }

    // ─── Tính năng chỉnh sửa phiếu ──────────────────────────────────────────

    /** Staff tự chỉnh sửa phiếu (chỉ khi status = DRAFT) */
    @PutMapping("/{id}/edit-staff")
    public ResponseEntity<ReceiptResponse> editReceiptByStaff(
            @PathVariable Integer id,
            @RequestBody EditReceiptRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.editReceiptByStaff(id, request, currentUser));
    }

    /** Manager chỉnh sửa phiếu + gửi xuống Staff (DRAFT) hoặc ghi cho Admin (PENDING_ADMIN) */
    @PutMapping("/{id}/edit-manager")
    public ResponseEntity<ReceiptResponse> editReceiptByManager(
            @PathVariable Integer id,
            @RequestBody EditReceiptRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.editReceiptByManager(id, request, currentUser));
    }

    /** Staff xác nhận thay đổi của Manager (khi status = PENDING_STAFF_CONFIRM) */
    @PostMapping("/{id}/acknowledge-edit")
    public ResponseEntity<ReceiptResponse> staffAcknowledgeEdit(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.staffAcknowledgeEdit(id, currentUser));
    }

    /** Lấy lịch sử chỉnh sửa của phiếu */
    @GetMapping("/{id}/edit-history")
    public ResponseEntity<List<ReceiptEditLogResponse>> getEditHistory(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.getEditHistory(id, currentUser));
    }
}

