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
    private final AuditLogService auditLogService;

    public CategoryServiceImpl(CategoryRepository categoryRepository, AuditLogService auditLogService) {
        this.categoryRepository = categoryRepository;
        this.auditLogService = auditLogService;
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

    private String normalizeString(String input) {
        if (input == null) return null;
        String s = input.replaceAll("[\u200B-\u200D\uFEFF]", "").replaceAll("\\s+", " ").trim().toLowerCase();
        s = s.replace("đ", "d");
        String unaccented = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        return java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(unaccented).replaceAll("");
    }

    @Override
    public org.springframework.data.domain.Page<CategoryResponse> getAllCategories(String keyword, int page) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")
        );
        org.springframework.data.domain.Page<Category> categoryPage;
        // Nếu có keyword truyền lên, thực hiện tìm kiếm theo tên không phân biệt hoa thường
        if (keyword != null && !keyword.trim().isEmpty()) {
            categoryPage = categoryRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        } else {
            // Nếu không có keyword, lấy toàn bộ danh sách
            categoryPage = categoryRepository.findAll(pageable);
        }
        // Chuyển đổi từ Entity sang DTO để trả về cho client
        return categoryPage.map(CategoryResponse::new);
    }

    @Override
    public CategoryResponse createCategory(CategorySaveRequest request, User currentUser) {
        // 1. Kiểm tra quyền của người dùng hiện tại
        checkPermission(currentUser);

        // 2. Kiểm tra tính hợp lệ của dữ liệu đầu vào
        if (request.getName() == null) {
            throw new RuntimeException("Tên danh mục không được để trống.");
        }
        String normalizedName = request.getName().replaceAll("[\u200B-\u200D\uFEFF]", "").replaceAll("\\s+", " ").trim();
        if (normalizedName.isEmpty()) {
            throw new RuntimeException("Tên danh mục không được để trống.");
        }

        // 3. Kiểm tra trùng lặp tên danh mục (bao gồm cả phân biệt hoa/thường, dấu tiếng Việt)
        String checkName = normalizeString(normalizedName);
        List<Category> allCategories = categoryRepository.findAll();
        for (Category c : allCategories) {
            if (normalizeString(c.getName()).equals(checkName)) {
                throw new RuntimeException("Tên bạn nhập bị trùng lặp hoặc quá giống với danh mục hiện có: '" + c.getName() + "'");
            }
        }

        // 4. Tạo thực thể mới và lưu vào cơ sở dữ liệu
        Category category = new Category();
        category.setName(normalizedName);

        category = categoryRepository.save(category);
        
        // Ghi log
        auditLogService.logAction(currentUser, "CREATE", "categories", 
                String.valueOf(category.getId()), 
                "Tạo mới danh mục: " + category.getName());

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
        if (request.getName() == null) {
            throw new RuntimeException("Tên danh mục không được để trống.");
        }
        String normalizedName = request.getName().replaceAll("[\u200B-\u200D\uFEFF]", "").replaceAll("\\s+", " ").trim();
        if (normalizedName.isEmpty()) {
            throw new RuntimeException("Tên danh mục không được để trống.");
        }

        // 4. Kiểm tra trùng lặp tên với các danh mục khác (chống lách luật hoa/thường, dấu)
        String checkName = normalizeString(normalizedName);
        List<Category> allCategories = categoryRepository.findAll();
        for (Category c : allCategories) {
            if (!c.getId().equals(id) && normalizeString(c.getName()).equals(checkName)) {
                throw new RuntimeException("Tên bạn nhập bị trùng lặp hoặc quá giống với danh mục hiện có: '" + c.getName() + "'");
            }
        }

        // 5. Cập nhật tên và lưu thay đổi
        category.setName(normalizedName);
        category = categoryRepository.save(category);

        // Ghi log
        auditLogService.logAction(currentUser, "UPDATE", "categories", 
                String.valueOf(category.getId()), 
                "Cập nhật danh mục: " + category.getName());

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
                
                // Ghi log
                auditLogService.logAction(currentUser, "DELETE", "categories", 
                        String.valueOf(category.getId()), 
                        "Xóa danh mục: " + category.getName());
            }
        } catch (DataIntegrityViolationException ex) {
            // Xử lý lỗi khi có ràng buộc khóa ngoại (sản phẩm đang thuộc danh mục này)
            throw new RuntimeException("Không thể xóa danh mục này vì đang có sản phẩm thuộc danh mục.");
        }
    }
}
