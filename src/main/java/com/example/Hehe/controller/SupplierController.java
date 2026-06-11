package com.example.Hehe.controller;

import com.example.Hehe.dto.SupplierResponse;
import com.example.Hehe.dto.SupplierSaveRequest;
import com.example.Hehe.model.User;
import com.example.Hehe.service.SupplierService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<?> searchSuppliers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        try {
            List<SupplierResponse> response = supplierService.searchSuppliers(keyword, status);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<?> getSupplierById(@PathVariable Integer id) {
        try {
            SupplierResponse response = supplierService.getSupplierById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> createSupplier(
            @RequestBody SupplierSaveRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            SupplierResponse response = supplierService.createSupplier(request, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateSupplier(
            @PathVariable Integer id,
            @RequestBody SupplierSaveRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            SupplierResponse response = supplierService.updateSupplier(id, request, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> deleteSupplier(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        try {
            supplierService.deleteSupplier(id, currentUser);
            return ResponseEntity.ok(Map.of("message", "Xóa nhà cung cấp thành công."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> toggleSupplierStatus(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        try {
            SupplierResponse response = supplierService.toggleSupplierStatus(id, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/adjust-debt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> adjustDebt(
            @PathVariable Integer id,
            @RequestBody Map<String, BigDecimal> body,
            @AuthenticationPrincipal User currentUser) {
        try {
            BigDecimal amount = body.get("amount");
            if (amount == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập số tiền điều chỉnh (amount)."));
            }
            SupplierResponse response = supplierService.adjustDebt(id, amount, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
