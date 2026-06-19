package com.example.Hehe.controller;

import com.example.Hehe.dto.InventoryResponse;
import com.example.Hehe.model.User;

import com.example.Hehe.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventories")
public class InventoryController {

    private final InventoryService inventoryService;
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getInventories(
            @RequestParam(required = false) Integer branchId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        List<InventoryResponse> res = inventoryService.getInventories(branchId, currentUser);
        return ResponseEntity.ok(res);
    }
    @PatchMapping("/{id}/add-stock")
    public ResponseEntity<InventoryResponse> addStock(
            @PathVariable Integer id,
            @RequestBody com.example.Hehe.dto.AddStockRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        InventoryResponse res = inventoryService.addStock(id, request.getQuantityToAdd(), currentUser);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{id}/expiry-warning")
    public ResponseEntity<InventoryResponse> updateExpiryWarning(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, Integer> body,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        InventoryResponse res = inventoryService.updateExpiryWarning(id, body.get("expiryWarningDays"), currentUser);
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(
            @RequestBody com.example.Hehe.dto.InventorySaveRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        InventoryResponse res = inventoryService.createInventory(request, currentUser);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/new-product")
    public ResponseEntity<InventoryResponse> createProductWithInventory(
            @RequestBody com.example.Hehe.dto.ProductWithInventoryRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        InventoryResponse res = inventoryService.createProductWithInventory(request, currentUser);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/global")
    public ResponseEntity<List<InventoryResponse>> getGlobalInventories() {
        return ResponseEntity.ok(inventoryService.getGlobalInventories());
    }
}
