package com.example.Hehe.service;

import com.example.Hehe.dto.ProductResponse;
import com.example.Hehe.dto.ProductSaveRequest;
import com.example.Hehe.model.User;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến quản lý Sản phẩm (Product).
 */
public interface ProductService {

    /**
     * Lấy danh sách sản phẩm, hỗ trợ tìm kiếm động theo nhiều tiêu chí.
     * @param keyword Từ khóa tìm kiếm theo tên (tương đối, không phân biệt hoa thường)
     * @param categoryId ID danh mục
     * @param minPrice Giá thấp nhất
     * @param maxPrice Giá cao nhất
     * @return Danh sách DTO hiển thị cho người dùng
     */
    List<ProductResponse> getAllProducts(String keyword, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Lấy chi tiết một sản phẩm theo ID.
     * @param id Khóa chính của sản phẩm
     * @return DTO chứa thông tin sản phẩm
     */
    ProductResponse getProductById(@NonNull Integer id);

    /**
     * Thêm mới một sản phẩm. Chỉ dành cho ADMIN.
     * @param request Dữ liệu từ form nhập liệu (bao gồm tên, danh mục, nsx, hsd...)
     * @param currentUser Người dùng đang thực hiện thao tác
     * @return Thông tin sản phẩm vừa tạo
     */
    ProductResponse createProduct(ProductSaveRequest request, User currentUser);

    /**
     * Cập nhật một sản phẩm đã có. Chỉ dành cho ADMIN.
     * @param id ID của sản phẩm cần cập nhật
     * @param request Dữ liệu cập nhật mới
     * @param currentUser Người dùng đang thực hiện thao tác
     * @return Thông tin sản phẩm sau khi cập nhật
     */
    ProductResponse updateProduct(@NonNull Integer id, ProductSaveRequest request, User currentUser);

    /**
     * Xóa một sản phẩm. Chỉ dành cho ADMIN.
     * @param id ID của sản phẩm cần xóa
     * @param currentUser Người dùng đang thực hiện thao tác
     */
    void deleteProduct(@NonNull Integer id, User currentUser);

    /**
     * Nhập hàng loạt sản phẩm từ file Excel.
     * @param file File Excel chứa dữ liệu
     * @param currentUser Người dùng thực hiện
     * @return Map chứa successCount và danh sách errors
     */
    Map<String, Object> importProductsFromExcel(MultipartFile file, User currentUser);

    /**
     * Tạo file Excel mẫu (template) để người dùng tải về.
     * @return mảng byte nội dung file Excel
     */
    byte[] generateExcelTemplate();
}
