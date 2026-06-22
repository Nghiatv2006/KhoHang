package com.example.Hehe.controller;

import com.example.Hehe.dto.ProductResponse;
import com.example.Hehe.dto.ProductSaveRequest;
import com.example.Hehe.model.User;
import com.example.Hehe.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller cung cấp các API (Endpoints) cho nghiệp vụ Product.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * API Lấy toàn bộ danh sách sản phẩm hoặc Tìm kiếm động.
     * Method: GET
     * Path: /api/products?keyword=...&categoryId=...&minPrice=...&maxPrice=...
     */
    @GetMapping
    public ResponseEntity<?> getAllProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        try {
            List<ProductResponse> products = productService.getAllProducts(keyword, categoryId, minPrice, maxPrice);
            return ResponseEntity.ok(products);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    /**
     * API Lấy chi tiết một sản phẩm theo ID.
     * Method: GET
     * Path: /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Integer id) {
        try {
            if (id == null) throw new RuntimeException("ID sản phẩm không được để trống.");
            ProductResponse product = productService.getProductById(id);
            return ResponseEntity.ok(product);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    /**
     * API Thêm mới sản phẩm. (Yêu cầu quyền ADMIN).
     * Method: POST
     * Path: /api/products
     */
    @PostMapping
    public ResponseEntity<?> createProduct(
            @RequestBody ProductSaveRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            ProductResponse response = productService.createProduct(request, currentUser);
            return ResponseEntity.ok(response);
        } catch (com.example.Hehe.exception.ProductDeletedConflictException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(Map.of(
                "code", "DELETED_CONFLICT",
                "message", ex.getMessage(),
                "productId", ex.getProductId()
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    /**
     * API Cập nhật sản phẩm. (Yêu cầu quyền ADMIN).
     * Method: PUT
     * Path: /api/products/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Integer id,
            @RequestBody ProductSaveRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (id == null) throw new RuntimeException("ID sản phẩm không được để trống.");
            ProductResponse response = productService.updateProduct(id, request, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    /**
     * API Xóa sản phẩm. (Yêu cầu quyền ADMIN).
     * Method: DELETE
     * Path: /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (id == null) throw new RuntimeException("ID sản phẩm không được để trống.");
            productService.deleteProduct(id, currentUser);
            return ResponseEntity.ok(Map.of("message", "Xóa sản phẩm thành công."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    /**
     * API Tải file Excel mẫu.
     * Method: GET
     * Path: /api/products/template
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> getExcelTemplate() {
        try {
            byte[] data = productService.generateExcelTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "Mau_Nhap_San_Pham.xlsx");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(data);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * API Nhập sản phẩm từ file Excel. (Yêu cầu quyền ADMIN).
     * Method: POST
     * Path: /api/products/import
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importProducts(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (file.isEmpty()) throw new RuntimeException("File tải lên trống.");
            Map<String, Object> result = productService.importProductsFromExcel(file, currentUser);
            return ResponseEntity.ok(result);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
