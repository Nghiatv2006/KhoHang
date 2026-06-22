package com.example.Hehe.controller;

import com.example.Hehe.dto.CustomerResponse;
import com.example.Hehe.dto.CustomerSaveRequest;
import com.example.Hehe.model.User;
import com.example.Hehe.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<?> searchCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User currentUser) {
        try {
            List<CustomerResponse> response = customerService.searchCustomers(keyword, status, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<?> getCustomerById(@PathVariable Integer id) {
        try {
            CustomerResponse response = customerService.getCustomerById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> createCustomer(
            @RequestBody CustomerSaveRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            CustomerResponse response = customerService.createCustomer(request, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateCustomer(
            @PathVariable Integer id,
            @RequestBody CustomerSaveRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            CustomerResponse response = customerService.updateCustomer(id, request, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> deleteCustomer(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        try {
            customerService.deleteCustomer(id, currentUser);
            return ResponseEntity.ok(Map.of("message", "Xóa khách hàng thành công."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> toggleCustomerStatus(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        try {
            CustomerResponse response = customerService.toggleCustomerStatus(id, currentUser);
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
            CustomerResponse response = customerService.adjustDebt(id, amount, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
