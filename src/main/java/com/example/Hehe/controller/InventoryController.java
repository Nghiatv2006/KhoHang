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
    @GetMapping("/global")
    public ResponseEntity<List<InventoryResponse>> getGlobalInventories() {
        return ResponseEntity.ok(inventoryService.getGlobalInventories());
    }
}
