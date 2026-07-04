package com.example.Hehe.controller;

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
    public ResponseEntity<ReceiptResponse> cancelReceipt(@PathVariable Integer id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.cancelReceipt(id, currentUser));
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
}
