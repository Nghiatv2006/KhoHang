package com.example.Hehe.service;

import com.example.Hehe.dto.InventoryResponse;
import com.example.Hehe.model.Inventory;
import com.example.Hehe.model.Branch;
import com.example.Hehe.model.Product;
import com.example.Hehe.model.Category;
import com.example.Hehe.model.User;
import com.example.Hehe.model.UserRole;
import com.example.Hehe.repository.InventoryRepository;
import com.example.Hehe.repository.BranchRepository;
import com.example.Hehe.repository.ProductRepository;
import com.example.Hehe.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@SuppressWarnings("null")
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @PersistenceContext
    private EntityManager entityManager;

    public InventoryServiceImpl(
            InventoryRepository inventoryRepository,
            BranchRepository branchRepository,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            AuditLogService auditLogService) {
        this.inventoryRepository = inventoryRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventories(Integer branchId, User currentUser) {
        List<Inventory> inventories;

        if (currentUser.getRole() == UserRole.ADMIN) {
            if (branchId != null) {
                inventories = inventoryRepository.findByBranchId(branchId);
            } else {
                inventories = inventoryRepository.findAll();
            }
        } else {
            // MANAGER and STAFF can only view their own branch
            if (currentUser.getBranch() == null) {
                throw new RuntimeException("Bạn chưa được phân công vào chi nhánh nào.");
            }
            Integer myBranchId = currentUser.getBranch().getId();
            
            if (branchId != null && !branchId.equals(myBranchId)) {
                throw new RuntimeException("Bạn chỉ có quyền xem tồn kho của chi nhánh mình.");
            }
            inventories = inventoryRepository.findByBranchId(myBranchId);
        }

        return inventories.stream()
                .map(InventoryResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public InventoryResponse addStock(Integer id, Integer quantityToAdd, User currentUser) {
        if (quantityToAdd == null || quantityToAdd <= 0) {
            throw new RuntimeException("Số lượng thêm vào phải lớn hơn 0.");
        }

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lô hàng tồn kho với ID: " + id));

        // Phân quyền: STAFF không được trực tiếp nhập thêm tồn kho
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Nhân viên không có quyền thực hiện thao tác này.");
        }

        // STAFF/MANAGER chỉ được chỉnh sửa tồn kho của chi nhánh mình
        if (currentUser.getRole() != UserRole.ADMIN) {
            if (currentUser.getBranch() == null || !currentUser.getBranch().getId().equals(inventory.getBranch().getId())) {
                throw new RuntimeException("Bạn không có quyền cập nhật tồn kho của chi nhánh này.");
            }
        }

        int oldQty = inventory.getQuantity();
        int newQty = oldQty + quantityToAdd;
        inventory.setQuantity(newQty);
        inventory.setLastUpdated(LocalDateTime.now());

        Inventory savedInventory = inventoryRepository.save(inventory);

        // Ghi audit log
        String productName = (inventory.getProduct() != null) ? inventory.getProduct().getName() : "Sản phẩm ẩn";
        String branchName = (inventory.getBranch() != null) ? inventory.getBranch().getName() : "Chi nhánh ẩn";
        String batchMsg = String.format("Nhập thêm %d sản phẩm '%s' vào lô '%s' tại '%s' (Số lượng cũ: %d -> mới: %d)",
                quantityToAdd, productName, inventory.getBatchCode(), branchName, oldQty, newQty);

        auditLogService.logAction(currentUser, "Nhập thêm hàng", "inventories", id.toString(), batchMsg);

        return new InventoryResponse(savedInventory);
    }

    @Override
    public InventoryResponse createInventory(com.example.Hehe.dto.InventorySaveRequest request, User currentUser) {
        if (request.getProductId() == null) {
            throw new RuntimeException("Sản phẩm không được để trống.");
        }
        if (request.getBranchId() == null) {
            throw new RuntimeException("Chi nhánh không được để trống.");
        }
        if (request.getBatchCode() == null || request.getBatchCode().trim().isEmpty()) {
            throw new RuntimeException("Mã lô sản xuất không được để trống.");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new RuntimeException("Số lượng ban đầu phải lớn hơn 0.");
        }

        // Kiểm tra quyền của currentUser
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Nhân viên không có quyền thực hiện thao tác này.");
        }
        if (currentUser.getRole() != UserRole.ADMIN) {
            if (currentUser.getBranch() == null || !currentUser.getBranch().getId().equals(request.getBranchId())) {
                throw new RuntimeException("Bạn không có quyền thêm tồn kho vào chi nhánh này.");
            }
        }

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh với ID: " + request.getBranchId()));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + request.getProductId()));

        if (request.getManufacturingDate() == null) {
            throw new RuntimeException("Ngày sản xuất (NSX) không được để trống.");
        }
        java.time.LocalDate mfg = request.getManufacturingDate();
        if (mfg.isAfter(java.time.LocalDate.now())) {
            throw new RuntimeException("Ngày sản xuất (NSX) không được lớn hơn ngày hiện tại.");
        }
        java.time.LocalDate exp = mfg; // Mặc định exp = mfg để thỏa mãn CHECK constraint exp_date >= mfg_date khi không có HSD
        boolean isExpiry = Boolean.TRUE.equals(request.getHasExpiry());

        if (isExpiry) {
            if (request.getExpirationDate() == null) {
                throw new RuntimeException("Sản phẩm quản lý theo hạn dùng bắt buộc phải điền HSD.");
            }
            if (request.getExpirationDate().isBefore(mfg)) {
                throw new RuntimeException("Hạn sử dụng không được nhỏ hơn Ngày sản xuất.");
            }
            exp = request.getExpirationDate();
        }

        // Kiểm tra xem lô tồn kho này đã có sẵn trong chi nhánh chưa
        java.util.Optional<Inventory> existingOpt = inventoryRepository.findByBranchIdAndProductIdAndBatchCode(
                request.getBranchId(), request.getProductId(), request.getBatchCode().trim());

        Inventory inventory;
        int oldQty = 0;
        if (existingOpt.isPresent()) {
            // Nếu đã tồn tại lô này -> Cộng dồn số lượng
            inventory = existingOpt.get();
            oldQty = inventory.getQuantity();
            inventory.setQuantity(oldQty + request.getQuantity());
            inventory.setManufacturingDate(mfg);
            inventory.setExpirationDate(exp);
            inventory.setHasExpiry(isExpiry);
            inventory.setExpiryWarningDays(request.getExpiryWarningDays() != null ? request.getExpiryWarningDays() : 30);
            inventory.setLastUpdated(LocalDateTime.now());
        } else {
            // Nếu chưa có -> Tạo mới
            inventory = new Inventory();
            inventory.setBranch(branch);
            inventory.setProduct(product);
            inventory.setBatchCode(request.getBatchCode().trim());
            inventory.setQuantity(request.getQuantity());
            inventory.setManufacturingDate(mfg);
            inventory.setExpirationDate(exp);
            inventory.setHasExpiry(isExpiry);
            inventory.setExpiryWarningDays(request.getExpiryWarningDays() != null ? request.getExpiryWarningDays() : 30);
            inventory.setLastUpdated(LocalDateTime.now());
        }

        Inventory savedInventory = inventoryRepository.save(inventory);

        // Ghi audit log
        String action = existingOpt.isPresent() ? "Nhập thêm hàng" : "Tạo lô hàng mới";
        String batchMsg = String.format("Nhập lô mới: %d sản phẩm '%s' (lô '%s') tại '%s' (Số lượng cũ: %d -> mới: %d)",
                request.getQuantity(), product.getName(), savedInventory.getBatchCode(), branch.getName(), oldQty, savedInventory.getQuantity());
        auditLogService.logAction(currentUser, action, "inventories", savedInventory.getId().toString(), batchMsg);

        return new InventoryResponse(savedInventory);
    }

    @Override
    public InventoryResponse createProductWithInventory(com.example.Hehe.dto.ProductWithInventoryRequest request, User currentUser) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên sản phẩm không được để trống.");
        }
        if (request.getPrice() == null || request.getPrice().doubleValue() < 0) {
            throw new RuntimeException("Giá bán không hợp lệ.");
        }
        if (request.getUnit() == null || request.getUnit().trim().isEmpty()) {
            throw new RuntimeException("Đơn vị tính không được để trống.");
        }
        if (request.getCategoryId() == null) {
            throw new RuntimeException("Vui lòng chọn danh mục.");
        }
        if (request.getBatchCode() == null || request.getBatchCode().trim().isEmpty()) {
            throw new RuntimeException("Mã lô sản xuất không được để trống.");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new RuntimeException("Số lượng nhập phải lớn hơn 0.");
        }

        // Kiểm tra quyền (chỉ ADMIN hoặc user có chi nhánh được phép tạo)
        // Mặc định tạo ở Kho Tổng (id = 1)
        Branch headBranch = branchRepository.findByIsHeadTrue().stream().findFirst()
                .orElseGet(() -> branchRepository.findById(1)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Kho Tổng trong hệ thống.")));

        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Nhân viên không có quyền thực hiện thao tác này.");
        }
        if (currentUser.getRole() != UserRole.ADMIN) {
            if (currentUser.getBranch() == null || !currentUser.getBranch().getId().equals(headBranch.getId())) {
                throw new RuntimeException("Bạn không có quyền thêm sản phẩm mới vào Kho Tổng.");
            }
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại."));

        // 1. Tạo và lưu sản phẩm mới
        Product product = new Product();
        // Tự sinh mã sản phẩm SKU (Spring Boot app uses product.setSku/getSku)
        product.setSku("PRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        product.setName(request.getName().trim());
        product.setPrice(request.getPrice());
        product.setUnit(request.getUnit().trim());
        product.setCategory(category);
        product.setHasExpiry(Boolean.TRUE.equals(request.getHasExpiry()));

        if (request.getManufacturingDate() == null) {
            throw new RuntimeException("Ngày sản xuất (NSX) không được để trống.");
        }
        java.time.LocalDate mfg = request.getManufacturingDate();
        if (mfg.isAfter(java.time.LocalDate.now())) {
            throw new RuntimeException("Ngày sản xuất (NSX) không được lớn hơn ngày hiện tại.");
        }
        java.time.LocalDate exp = mfg; // Mặc định exp = mfg để thỏa mãn CHECK constraint exp_date >= mfg_date khi không có HSD

        if (Boolean.TRUE.equals(request.getHasExpiry())) {
            if (request.getExpirationDate() == null) {
                throw new RuntimeException("Sản phẩm quản lý theo hạn dùng bắt buộc phải nhập HSD.");
            }
            if (request.getExpirationDate().isBefore(mfg)) {
                throw new RuntimeException("Hạn sử dụng không được nhỏ hơn Ngày sản xuất.");
            }
            exp = request.getExpirationDate();
        }
        
        product.setManufacturingDate(mfg);
        product.setExpirationDate(exp);

        Product savedProduct = productRepository.save(product);

        // 2. Tạo và lưu Tồn kho ban đầu tại Kho Tổng
        Inventory inventory = new Inventory();
        inventory.setBranch(headBranch);
        inventory.setProduct(savedProduct);
        inventory.setBatchCode(request.getBatchCode().trim());
        inventory.setQuantity(request.getQuantity());
        inventory.setManufacturingDate(mfg);
        inventory.setExpirationDate(exp);
        inventory.setLastUpdated(LocalDateTime.now());

        Inventory savedInventory = inventoryRepository.save(inventory);

        // Ghi Audit log
        String detailsMsg = String.format("Tạo mới sản phẩm '%s' (Mã: %s, ĐVT: %s) và nhập %d sản phẩm vào lô '%s' tại Kho Tổng.",
                savedProduct.getName(), savedProduct.getSku(), savedProduct.getUnit(), request.getQuantity(), savedInventory.getBatchCode());
        auditLogService.logAction(currentUser, "Tạo SP & Nhập kho", "products", savedProduct.getId().toString(), detailsMsg);

        return new InventoryResponse(savedInventory);
    }

    @Override
    public InventoryResponse updateExpiryWarning(Integer id, Integer expiryWarningDays, User currentUser) {
        if (expiryWarningDays == null || expiryWarningDays <= 0) {
            throw new RuntimeException("Số ngày cảnh báo hạn dùng phải lớn hơn 0.");
        }

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lô hàng tồn kho với ID: " + id));

        // Phân quyền: STAFF không được phép cập nhật cấu hình cảnh báo hạn dùng
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Nhân viên không có quyền thực hiện thao tác này.");
        }

        // STAFF/MANAGER chỉ được chỉnh sửa cấu hình của chi nhánh mình
        if (currentUser.getRole() != UserRole.ADMIN) {
            if (currentUser.getBranch() == null || !currentUser.getBranch().getId().equals(inventory.getBranch().getId())) {
                throw new RuntimeException("Bạn không có quyền cập nhật cấu hình của chi nhánh này.");
            }
        }

        if (!Boolean.TRUE.equals(inventory.getHasExpiry())) {
            throw new RuntimeException("Lô hàng này không cấu hình quản lý theo hạn dùng.");
        }

        int oldDays = inventory.getExpiryWarningDays() != null ? inventory.getExpiryWarningDays() : 30;
        inventory.setExpiryWarningDays(expiryWarningDays);
        inventory.setLastUpdated(LocalDateTime.now());

        Inventory savedInventory = inventoryRepository.save(inventory);

        // Ghi audit log
        String batchMsg = String.format("Cập nhật số ngày cảnh báo hạn dùng lô '%s' (Sản phẩm: '%s') từ %d ngày thành %d ngày",
                inventory.getBatchCode(), inventory.getProduct().getName(), oldDays, expiryWarningDays);
        auditLogService.logAction(currentUser, "Sửa cảnh báo HSD", "inventories", id.toString(), batchMsg);

        return new InventoryResponse(savedInventory);
    }




    @Override
    public void deleteInventory(Integer id, User currentUser) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dòng tồn kho với ID: " + id));

        // Kiểm tra quyền: STAFF không được phép xoá tồn kho
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Nhân viên không có quyền thực hiện thao tác này.");
        }

        // chỉ ADMIN hoặc Quản lý của chi nhánh đó mới được xoá
        if (currentUser.getRole() != com.example.Hehe.model.UserRole.ADMIN) {
            if (currentUser.getBranch() == null || !currentUser.getBranch().getId().equals(inventory.getBranch().getId())) {
                throw new RuntimeException("Bạn không có quyền xoá dữ liệu tồn kho của chi nhánh này.");
            }
        }

        String details = String.format("Xóa lô hàng '%s' (Sản phẩm: '%s') khỏi chi nhánh '%s'",
                inventory.getBatchCode(), inventory.getProduct().getName(), inventory.getBranch().getName());
        auditLogService.logAction(currentUser, "Xóa lô hàng", "inventories", id.toString(), details);

        inventoryRepository.delete(inventory);
    }
}
