package com.example.Hehe.controller;

import com.example.Hehe.dto.BranchResponse;
import com.example.Hehe.model.Branch;
import com.example.Hehe.model.User;
import com.example.Hehe.model.UserRole;
import com.example.Hehe.repository.BranchRepository;
import com.example.Hehe.repository.UserRepository;
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
    private final UserRepository userRepository;

    public BranchController(BranchRepository branchRepository, UserRepository userRepository) {
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllBranches(@RequestParam(required = false) String keyword) {
        try {
            List<Branch> branches;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String cleanKeyword = keyword.trim();
                branches = branchRepository.findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(cleanKeyword, cleanKeyword);
            } else {
                branches = branchRepository.findAll();
            }
            List<BranchResponse> responses = branches.stream()
                    .map(this::convertToResponse)
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBranchById(@PathVariable Integer id) {
        try {
            Branch branch = branchRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh với ID: " + id));
            return ResponseEntity.ok(convertToResponse(branch));
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
        if (branch.getTaxCode() == null || branch.getTaxCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã số thuế không được để trống."));
        }
        String cleanTaxCode = branch.getTaxCode().trim();
        if (!cleanTaxCode.matches("^[0-9A-Za-z-]{10,13}$")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã số thuế không hợp lệ (phải từ 10 đến 13 ký tự chữ/số/dấu gạch)."));
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
            branch.setTaxCode(cleanTaxCode);
            if (branch.getIsHead() == null) {
                branch.setIsHead(false);
            }
            
            Branch savedBranch = branchRepository.save(branch);
            
            if (Boolean.TRUE.equals(savedBranch.getIsHead())) {
                branchRepository.demoteOtherHeadBranches(savedBranch.getId());
            }
            
            return ResponseEntity.ok(convertToResponse(savedBranch));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBranch(
            @PathVariable Integer id,
            @RequestBody Branch requestBody,
            @AuthenticationPrincipal User currentUser) {

        // Quyền sửa chi nhánh: ADMIN được sửa mọi thông tin. MANAGER/STAFF chỉ được sửa ngưỡng tồn kho của mình.
        if (currentUser.getRole() != UserRole.ADMIN) {
            if (currentUser.getBranch() == null || !currentUser.getBranch().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Bạn không có quyền chỉnh sửa chi nhánh này."));
            }
        }

        // Validate dữ liệu bắt buộc đối với ADMIN
        if (currentUser.getRole() == UserRole.ADMIN) {
            if (requestBody.getName() == null || requestBody.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tên chi nhánh không được để trống."));
            }
            if (requestBody.getAddress() == null || requestBody.getAddress().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Địa chỉ chi nhánh không được để trống."));
            }
            if (requestBody.getTaxCode() == null || requestBody.getTaxCode().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mã số thuế không được để trống."));
            }
            String cleanTaxCode = requestBody.getTaxCode().trim();
            if (!cleanTaxCode.matches("^[0-9A-Za-z-]{10,13}$")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mã số thuế không hợp lệ (phải từ 10 đến 13 ký tự chữ/số/dấu gạch)."));
            }
        }

        if (requestBody.getLowStockThreshold() == null || requestBody.getLowStockThreshold() < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ngưỡng tồn kho tối thiểu không hợp lệ."));
        }

        try {
            Branch branch = branchRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh với ID: " + id));

            if (currentUser.getRole() != UserRole.ADMIN) {
                // Kiểm tra xem MANAGER/STAFF có cố tình chỉnh sửa thông tin khác không
                if (requestBody.getName() != null && !requestBody.getName().trim().equals(branch.getName())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Bạn chỉ có quyền thay đổi ngưỡng tồn kho tối thiểu."));
                }
                if (requestBody.getAddress() != null && !requestBody.getAddress().trim().equals(branch.getAddress())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Bạn chỉ có quyền thay đổi ngưỡng tồn kho tối thiểu."));
                }
                if (requestBody.getTaxCode() != null && !requestBody.getTaxCode().trim().equals(branch.getTaxCode())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Bạn chỉ có quyền thay đổi ngưỡng tồn kho tối thiểu."));
                }
                
                branch.setLowStockThreshold(requestBody.getLowStockThreshold());
            } else {
                // ADMIN cập nhật đầy đủ thông tin
                if (branchRepository.existsByNameAndIdNot(requestBody.getName().trim(), id)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Tên chi nhánh '" + requestBody.getName().trim() + "' đã tồn tại."));
                }
                branch.setName(requestBody.getName().trim());
                branch.setAddress(requestBody.getAddress().trim());
                branch.setLowStockThreshold(requestBody.getLowStockThreshold());
                branch.setTaxCode(requestBody.getTaxCode().trim());
                if (requestBody.getIsHead() != null) {
                    branch.setIsHead(requestBody.getIsHead());
                }
            }

            Branch updatedBranch = branchRepository.save(branch);
            
            if (Boolean.TRUE.equals(updatedBranch.getIsHead())) {
                branchRepository.demoteOtherHeadBranches(updatedBranch.getId());
            }
            
            return ResponseEntity.ok(convertToResponse(updatedBranch));
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

            if (Boolean.TRUE.equals(branch.getIsHead())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Không được phép xóa chi nhánh tổng."));
            }

            branchRepository.delete(branch);
            return ResponseEntity.ok(Map.of("message", "Xóa chi nhánh thành công."));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chi nhánh đã phát sinh dữ liệu giao dịch hoặc có nhân sự/sản phẩm liên kết, không thể xóa."));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    private String getManagerNameForBranch(Branch branch) {
        if (Boolean.TRUE.equals(branch.getIsHead())) {
            List<User> admins = userRepository.searchUsers(null, UserRole.ADMIN, null, null);
            if (admins.isEmpty()) {
                return "Chưa phân công";
            }
            return admins.stream().map(User::getFullName).collect(java.util.stream.Collectors.joining(", "));
        } else {
            List<User> managers = userRepository.searchUsers(null, UserRole.MANAGER, branch.getId(), null);
            if (managers.isEmpty()) {
                return "Chưa phân công";
            }
            return managers.stream().map(User::getFullName).collect(java.util.stream.Collectors.joining(", "));
        }
    }

    private BranchResponse convertToResponse(Branch b) {
        return new BranchResponse(
            b.getId(),
            b.getName(),
            b.getAddress(),
            b.getLowStockThreshold(),
            b.getIsHead(),
            b.getTaxCode(),
            getManagerNameForBranch(b)
        );
    }
}
