package com.example.Hehe.controller;

import com.example.Hehe.model.Branch;
import com.example.Hehe.model.User;
import com.example.Hehe.model.UserRole;
import com.example.Hehe.repository.BranchRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchRepository branchRepository;

    public BranchController(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllBranches(@RequestParam(required = false) String keyword) {
        try {
            if (keyword != null && !keyword.trim().isEmpty()) {
                String cleanKeyword = keyword.trim();
                List<Branch> branches = branchRepository.findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(cleanKeyword, cleanKeyword);
                return ResponseEntity.ok(branches);
            }
            return ResponseEntity.ok(branchRepository.findAll());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBranchById(@PathVariable Integer id) {
        try {
            Branch branch = branchRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh với ID: " + id));
            return ResponseEntity.ok(branch);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createBranch(
            @RequestBody Branch branch,
            @AuthenticationPrincipal User currentUser) {
        
        // Chỉ ADMIN mới có quyền thêm chi nhánh
        if (currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Chỉ ADMIN mới có quyền tạo mới chi nhánh."));
        }

        // Validate dữ liệu bắt buộc
        if (branch.getName() == null || branch.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên chi nhánh không được để trống."));
        }
        if (branch.getAddress() == null || branch.getAddress().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Địa chỉ chi nhánh không được để trống."));
        }
        if (branch.getLowStockThreshold() == null || branch.getLowStockThreshold() < 0) {
            branch.setLowStockThreshold(5); // Default fallback
        }

        // Kiểm tra trùng tên
        if (branchRepository.existsByName(branch.getName().trim())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên chi nhánh '" + branch.getName().trim() + "' đã tồn tại."));
        }

        try {
            branch.setName(branch.getName().trim());
            branch.setAddress(branch.getAddress().trim());
            Branch savedBranch = branchRepository.save(branch);
            return ResponseEntity.ok(savedBranch);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBranch(
            @PathVariable Integer id,
            @RequestBody Branch requestBody,
            @AuthenticationPrincipal User currentUser) {

        // Chỉ ADMIN mới có quyền sửa chi nhánh
        if (currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Chỉ ADMIN mới có quyền chỉnh sửa chi nhánh."));
        }

        // Validate dữ liệu bắt buộc
        if (requestBody.getName() == null || requestBody.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên chi nhánh không được để trống."));
        }
        if (requestBody.getAddress() == null || requestBody.getAddress().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Địa chỉ chi nhánh không được để trống."));
        }
        if (requestBody.getLowStockThreshold() == null || requestBody.getLowStockThreshold() < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ngưỡng tồn kho tối thiểu không hợp lệ."));
        }

        try {
            Branch branch = branchRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh với ID: " + id));

            // Kiểm tra trùng tên (loại trừ chính nó)
            if (branchRepository.existsByNameAndIdNot(requestBody.getName().trim(), id)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tên chi nhánh '" + requestBody.getName().trim() + "' đã tồn tại."));
            }

            branch.setName(requestBody.getName().trim());
            branch.setAddress(requestBody.getAddress().trim());
            branch.setLowStockThreshold(requestBody.getLowStockThreshold());

            Branch updatedBranch = branchRepository.save(branch);
            return ResponseEntity.ok(updatedBranch);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBranch(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {

        // Chỉ ADMIN mới có quyền xóa chi nhánh
        if (currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Chỉ ADMIN mới có quyền xóa chi nhánh."));
        }

        try {
            Branch branch = branchRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh với ID: " + id));

            branchRepository.delete(branch);
            return ResponseEntity.ok(Map.of("message", "Xóa chi nhánh thành công."));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chi nhánh đã phát sinh dữ liệu giao dịch hoặc có nhân sự/sản phẩm liên kết, không thể xóa."));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
