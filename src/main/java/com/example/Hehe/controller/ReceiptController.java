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

    @PostMapping
    public ResponseEntity<ReceiptResponse> createReceipt(@RequestBody ReceiptSaveRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.createReceipt(request, currentUser));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReceiptResponse> cancelReceipt(@PathVariable Integer id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptService.cancelReceipt(id, currentUser));
    }
}
