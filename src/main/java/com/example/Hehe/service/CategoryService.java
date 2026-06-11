package com.example.Hehe.service;

import com.example.Hehe.dto.CategoryResponse;
import com.example.Hehe.dto.CategorySaveRequest;
import com.example.Hehe.model.User;

import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến danh mục sản phẩm (Category).
 */
public interface CategoryService {
    
    /**
     * Lấy danh sách tất cả các danh mục, có hỗ trợ tìm kiếm theo từ khóa.
     * @param keyword Từ khóa tìm kiếm (có thể null)
     * @return Danh sách các danh mục (DTO)
     */
    List<CategoryResponse> getAllCategories(String keyword);

    /**
     * Thêm mới một danh mục.
     * Yêu cầu quyền: ADMIN hoặc MANAGER.
     * @param request Dữ liệu danh mục mới được gửi lên từ client
     * @param currentUser Người dùng hiện tại đang thực hiện thao tác
     * @return Thông tin danh mục vừa tạo thành công (DTO)
     */
    CategoryResponse createCategory(CategorySaveRequest request, User currentUser);

    /**
     * Cập nhật thông tin một danh mục đã tồn tại.
     * Yêu cầu quyền: ADMIN hoặc MANAGER.
     * @param id ID của danh mục cần cập nhật
     * @param request Dữ liệu danh mục mới
     * @param currentUser Người dùng hiện tại đang thực hiện thao tác
     * @return Thông tin danh mục sau khi đã cập nhật thành công (DTO)
     */
    CategoryResponse updateCategory(Integer id, CategorySaveRequest request, User currentUser);

    /**
     * Xóa một danh mục.
     * Yêu cầu quyền: ADMIN hoặc MANAGER.
     * Không cho phép xóa nếu danh mục đang được sử dụng ở bảng sản phẩm (ràng buộc khóa ngoại).
     * @param id ID của danh mục cần xóa
     * @param currentUser Người dùng hiện tại đang thực hiện thao tác
     */
    void deleteCategory(Integer id, User currentUser);
}
