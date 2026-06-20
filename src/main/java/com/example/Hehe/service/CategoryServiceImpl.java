package com.example.Hehe.service;

import com.example.Hehe.dto.CategoryResponse;
import com.example.Hehe.dto.CategorySaveRequest;
import com.example.Hehe.model.Category;
import com.example.Hehe.model.User;
import com.example.Hehe.model.UserRole;
import com.example.Hehe.repository.CategoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lớp implementation thực hiện logic cho CategoryService.
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Hàm dùng chung để kiểm tra quyền truy cập.
     * Chỉ ADMIN và MANAGER của Chi nhánh Hà Nội (branchId = 1) mới được phép thay đổi danh mục.
     * @param currentUser User đang gọi thao tác
     */
    private void checkPermission(User currentUser) {
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isMainBranchManager = currentUser.getRole() == UserRole.MANAGER 
                                      && currentUser.getBranch() != null 
                                      && currentUser.getBranch().getId() == 1;

        if (!isAdmin && !isMainBranchManager) {
            throw new RuntimeException("Bạn không có quyền thực hiện thao tác này. Yêu cầu quyền ADMIN hoặc MANAGER Chi nhánh Hà Nội.");
        }
    }

    @Override
    public List<CategoryResponse> getAllCategories(String keyword) {
        List<Category> categories;
        // Nếu có keyword truyền lên, thực hiện tìm kiếm theo tên không phân biệt hoa thường
        if (keyword != null && !keyword.trim().isEmpty()) {
            categories = categoryRepository.findByNameContainingIgnoreCase(keyword.trim());
        } else {
            // Nếu không có keyword, lấy toàn bộ danh sách
            categories = categoryRepository.findAll();
        }
        // Chuyển đổi từ Entity sang DTO để trả về cho client
        return categories.stream().map(CategoryResponse::new).collect(Collectors.toList());
    }

    @Override
    public CategoryResponse createCategory(CategorySaveRequest request, User currentUser) {
        // 1. Kiểm tra quyền của người dùng hiện tại
        checkPermission(currentUser);

        // 2. Kiểm tra tính hợp lệ của dữ liệu đầu vào (tên danh mục không rỗng)
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên danh mục không được để trống.");
        }

        // 3. Kiểm tra trùng lặp tên danh mục
        if (categoryRepository.existsByName(request.getName().trim())) {
            throw new RuntimeException("Tên danh mục đã tồn tại.");
        }

        // 4. Tạo thực thể mới và lưu vào cơ sở dữ liệu
        Category category = new Category();
        category.setName(request.getName().trim());

        category = categoryRepository.save(category);
        
        // 5. Trả về DTO
        return new CategoryResponse(category);
    }

    @Override
    public CategoryResponse updateCategory(Integer id, CategorySaveRequest request, User currentUser) {
        // 1. Kiểm tra quyền của người dùng hiện tại
        checkPermission(currentUser);
        
        if (id == null) {
            throw new RuntimeException("ID danh mục không được để trống.");
        }

        // 2. Tìm danh mục trong cơ sở dữ liệu
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục."));

        // 3. Kiểm tra tính hợp lệ của dữ liệu gửi lên
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên danh mục không được để trống.");
        }

        String newName = request.getName().trim();
        
        // 4. Kiểm tra trùng lặp tên với các danh mục khác
        if (!category.getName().equalsIgnoreCase(newName) && categoryRepository.existsByName(newName)) {
            throw new RuntimeException("Tên danh mục đã tồn tại.");
        }

        // 5. Cập nhật tên và lưu thay đổi
        category.setName(newName);
        category = categoryRepository.save(category);

        // 6. Trả về thông tin danh mục sau khi đã sửa
        return new CategoryResponse(category);
    }

    @Override
    public void deleteCategory(Integer id, User currentUser) {
        // 1. Kiểm tra quyền của người dùng hiện tại
        checkPermission(currentUser);
        
        if (id == null) {
            throw new RuntimeException("ID danh mục không được để trống.");
        }

        // 2. Tìm danh mục cần xóa
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục."));

        try {
            // 3. Xóa danh mục khỏi cơ sở dữ liệu
            if (category != null) {
                categoryRepository.delete(category);
            }
        } catch (DataIntegrityViolationException ex) {
            // Xử lý lỗi khi có ràng buộc khóa ngoại (sản phẩm đang thuộc danh mục này)
            throw new RuntimeException("Không thể xóa danh mục này vì đang có sản phẩm thuộc danh mục.");
        }
    }
}
