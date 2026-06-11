package com.example.Hehe.service;

import com.example.Hehe.dto.ProductResponse;
import com.example.Hehe.dto.ProductSaveRequest;
import com.example.Hehe.model.Category;
import com.example.Hehe.model.Product;
import com.example.Hehe.model.User;
import com.example.Hehe.model.UserRole;
import com.example.Hehe.repository.CategoryRepository;
import com.example.Hehe.repository.ProductRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Class thực thi các nghiệp vụ của ProductService.
 */
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Hàm dùng chung để kiểm tra quyền truy cập.
     * Dựa theo kế hoạch, hiện tại chỉ có ADMIN mới được phép thao tác CRUD trên sản phẩm.
     */
    private void checkAdminPermission(User currentUser) {
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Bạn không có quyền thực hiện thao tác này. Yêu cầu quyền ADMIN.");
        }
    }

    /**
     * Lấy danh sách sản phẩm, hỗ trợ tìm kiếm động theo nhiều tiêu chí.
     * Bất kỳ ai (đã đăng nhập) cũng có thể xem và tìm kiếm danh sách sản phẩm.
     */
    @Override
    public List<ProductResponse> getAllProducts(String keyword, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tìm kiếm theo tên (không phân biệt chữ hoa, chữ thường)
            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%"));
            }

            // Lọc theo danh mục
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // Lọc theo khoảng giá
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Product> products = productRepository.findAll(spec);
        
        // Chuyển List<Product> thành List<ProductResponse>
        return products.stream()
                .map(ProductResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết một sản phẩm.
     */
    @Override
    public ProductResponse getProductById(@NonNull Integer id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        return new ProductResponse(product);
    }

    /**
     * Thêm mới một sản phẩm.
     * Hàm này quan trọng vì nó thực hiện:
     * - Sinh mã SKU tự động
     * - Kiểm tra logic danh mục
     * - Kiểm tra logic ngày sản xuất (NSX) và hạn sử dụng (HSD)
     */
    @Override
    public ProductResponse createProduct(ProductSaveRequest request, User currentUser) {
        checkAdminPermission(currentUser);

        // Kiểm tra xem Category có tồn tại không
        Integer categoryId = request.getCategoryId();
        if (categoryId == null) {
            throw new RuntimeException("ID danh mục không được để trống.");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại trong hệ thống."));

        // Kiểm tra ngày sản xuất và hạn sử dụng (nếu có nhập cả 2)
        if (request.getManufacturingDate() != null && request.getExpirationDate() != null) {
            if (request.getManufacturingDate().isAfter(request.getExpirationDate())) {
                throw new RuntimeException("Ngày sản xuất không thể lớn hơn hạn sử dụng.");
            }
        }

        Product product = new Product();
        // Sinh SKU tự động: Kết hợp tiền tố PRD- với một chuỗi UUID ngẫu nhiên (cắt ngắn)
        product.setSku("PRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        
        // Gán các trường khác
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity() == null ? 0 : request.getQuantity());
        product.setDescription(request.getDescription());
        product.setCategory(category);
        product.setManufacturingDate(request.getManufacturingDate());
        product.setExpirationDate(request.getExpirationDate());
        
        // Mặc định cho các trường bắt buộc của schema cũ
        product.setUnit("Chiếc"); 
        product.setHasExpiry(request.getExpirationDate() != null);

        // Lưu vào DB
        Product savedProduct = productRepository.save(product);

        return new ProductResponse(savedProduct);
    }

    /**
     * Cập nhật thông tin sản phẩm.
     * Lưu ý: Không cho phép cập nhật mã SKU.
     */
    @Override
    public ProductResponse updateProduct(@NonNull Integer id, ProductSaveRequest request, User currentUser) {
        checkAdminPermission(currentUser);


        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        // Kiểm tra danh mục
        Integer categoryId = request.getCategoryId();
        if (categoryId == null) throw new RuntimeException("ID danh mục không được để trống.");
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại trong hệ thống."));

        // Kiểm tra ngày sản xuất và hạn sử dụng (nếu có nhập cả 2)
        if (request.getManufacturingDate() != null && request.getExpirationDate() != null) {
            if (request.getManufacturingDate().isAfter(request.getExpirationDate())) {
                throw new RuntimeException("Ngày sản xuất không thể lớn hơn hạn sử dụng.");
            }
        }

        // Cập nhật thông tin (Bỏ qua SKU)
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity() != null ? request.getQuantity() : product.getQuantity());
        product.setDescription(request.getDescription());
        product.setCategory(category);
        product.setManufacturingDate(request.getManufacturingDate());
        product.setExpirationDate(request.getExpirationDate());
        if (request.getExpirationDate() != null) {
            product.setHasExpiry(true);
        }

        Product updatedProduct = productRepository.save(product);

        return new ProductResponse(updatedProduct);
    }

    /**
     * Xóa sản phẩm khỏi hệ thống.
     */
    @Override
    public void deleteProduct(@NonNull Integer id, User currentUser) {
        checkAdminPermission(currentUser);


        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        if (product != null) {
            productRepository.delete(product);
        }
    }
}
