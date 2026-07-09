package com.example.Hehe.controller;

import com.example.Hehe.dto.CategoryResponse;
import com.example.Hehe.dto.CategorySaveRequest;
import com.example.Hehe.model.User;
import com.example.Hehe.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller cho các API liên quan đến Quản lý danh mục (Category).
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * API Lấy danh sách danh mục (Có thể truyền keyword để tìm kiếm)
     * Method: GET
     * Path: /api/categories hoặc /api/categories?keyword=abc
     */
    @GetMapping
    public ResponseEntity<?> getAllCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page) {
        try {
            org.springframework.data.domain.Page<CategoryResponse> categories = categoryService.getAllCategories(keyword, page);
            return ResponseEntity.ok(categories);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    /**
     * API Thêm mới một danh mục
     * Method: POST
     * Path: /api/categories
     */
    @PostMapping
    public ResponseEntity<?> createCategory(
            @RequestBody CategorySaveRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            // Chuyển request và thông tin người dùng đang đăng nhập tới Service xử lý
            CategoryResponse response = categoryService.createCategory(request, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            // Trả về lỗi định dạng JSON nếu có exception nghiệp vụ
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    /**
     * API Cập nhật thông tin danh mục hiện có
     * Method: PUT
     * Path: /api/categories/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Integer id,
            @RequestBody CategorySaveRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            CategoryResponse response = categoryService.updateCategory(id, request, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    /**
     * API Xóa một danh mục
     * Method: DELETE
     * Path: /api/categories/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        try {
            categoryService.deleteCategory(id, currentUser);
            // Nếu xóa thành công, trả về message báo OK
            return ResponseEntity.ok(Map.of("message", "Xóa danh mục thành công."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
