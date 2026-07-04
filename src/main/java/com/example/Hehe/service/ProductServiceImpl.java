package com.example.Hehe.service;

import com.example.Hehe.dto.ProductResponse;
import com.example.Hehe.dto.ProductSaveRequest;
import com.example.Hehe.model.*;
import com.example.Hehe.repository.CategoryRepository;
import com.example.Hehe.repository.ProductRepository;
import com.example.Hehe.repository.BranchRepository;
import com.example.Hehe.repository.InventoryRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

/**
 * Class thực thi các nghiệp vụ của ProductService.
 */
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BranchRepository branchRepository;
    private final InventoryRepository inventoryRepository;
    private final com.example.Hehe.repository.ReceiptDetailRepository receiptDetailRepository;
    private final AuditLogService auditLogService;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              BranchRepository branchRepository,
                              InventoryRepository inventoryRepository,
                              com.example.Hehe.repository.ReceiptDetailRepository receiptDetailRepository,
                              AuditLogService auditLogService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.branchRepository = branchRepository;
        this.inventoryRepository = inventoryRepository;
        this.receiptDetailRepository = receiptDetailRepository;
        this.auditLogService = auditLogService;
    }


    /**
     * Hàm dùng chung để kiểm tra quyền truy cập.
     * Chỉ ADMIN và MANAGER của Chi nhánh Hà Nội (branchId = 1) mới được phép thay đổi sản phẩm.
     */
    private void checkPermission(User currentUser) {
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isAdmin) {
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

            // Chỉ lấy các sản phẩm chưa bị xóa (Soft Delete)
            predicates.add(cb.or(cb.isNull(root.get("isDeleted")), cb.equal(root.get("isDeleted"), false)));

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
        checkPermission(currentUser);

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
        if (!Boolean.TRUE.equals(request.getForceCreate())) {
            java.util.Optional<Product> deletedProduct = productRepository.findFirstByNameAndIsDeletedTrue(request.getName());
            if (deletedProduct.isPresent()) {
                throw new com.example.Hehe.exception.ProductDeletedConflictException(
                    deletedProduct.get().getId(),
                    "Sản phẩm này từng tồn tại và đã bị xóa. Bạn có muốn khôi phục lại nó không hay vẫn muốn tạo một mã sản phẩm mới hoàn toàn?"
                );
            }
        }

        Product product = new Product();
        // Sinh SKU tự động: Kết hợp tiền tố PRD- với một chuỗi UUID ngẫu nhiên (cắt ngắn)
        product.setSku("PRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        
        // Gán các trường khác
        product.setName(request.getName());
        product.setImportPrice(request.getImportPrice() != null ? request.getImportPrice() : BigDecimal.ZERO);
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setManufacturingDate(request.getManufacturingDate());
        product.setExpirationDate(request.getExpirationDate());
        
        // Mặc định cho các trường bắt buộc của schema cũ
        product.setUnit(request.getUnit() != null && !request.getUnit().trim().isEmpty() ? request.getUnit().trim() : "Chiếc"); 
        product.setHasExpiry(request.getHasExpiry() != null ? request.getHasExpiry() : false);

        // Lưu vào DB
        Product savedProduct = productRepository.save(product);

        // Ghi Nhật ký
        auditLogService.logAction(currentUser, "CREATE", "products",
                String.valueOf(savedProduct.getId()),
                "Tạo mới sản phẩm: " + savedProduct.getName());

        return new ProductResponse(savedProduct);
    }

    /**
     * Cập nhật thông tin sản phẩm.
     * Lưu ý: Không cho phép cập nhật mã SKU.
     */
    @Override
    public ProductResponse updateProduct(@NonNull Integer id, ProductSaveRequest request, User currentUser) {
        checkPermission(currentUser);


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

        // Kiểm tra thay đổi cài đặt Hạn sử dụng (hasExpiry)
        boolean oldHasExpiry = product.getHasExpiry() != null && product.getHasExpiry();
        boolean newHasExpiry = request.getExpirationDate() != null;
        if (oldHasExpiry != newHasExpiry) {
            boolean hasStock = inventoryRepository.existsByProductIdAndQuantityGreaterThan(product.getId(), 0);
            boolean hasTransactions = receiptDetailRepository.existsByProductId(product.getId());
            if (hasStock || hasTransactions) {
                throw new RuntimeException("Không được phép thay đổi cài đặt Hạn sử dụng (hasExpiry) của sản phẩm đã phát sinh tồn kho hoặc giao dịch.");
            }
            product.setHasExpiry(newHasExpiry);
        }

        // Delete old image if it has changed and is not null
        String oldImageUrl = product.getImageUrl();
        String newImageUrl = request.getImageUrl();
        if (oldImageUrl != null && !oldImageUrl.equals(newImageUrl)) {
            deletePhysicalImage(oldImageUrl);
        }

        // Cập nhật thông tin (Bỏ qua SKU)
        product.setName(request.getName());
        if (request.getImportPrice() != null) {
            product.setImportPrice(request.getImportPrice());
        }
        product.setPrice(request.getPrice());
        product.setImageUrl(newImageUrl);
        product.setCategory(category);
        if (newHasExpiry) {
            product.setManufacturingDate(request.getManufacturingDate());
            product.setExpirationDate(request.getExpirationDate());
        } else {
            product.setManufacturingDate(LocalDate.of(1970, 1, 1));
            product.setExpirationDate(LocalDate.of(1970, 1, 1));
        }

        // Khôi phục lại nếu sản phẩm đang bị xóa mềm
        boolean isRestoring = false;
        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            product.setIsDeleted(false);
            isRestoring = true;
        }

        Product updatedProduct = productRepository.save(product);

        // Ghi Nhật ký
        if (isRestoring) {
            auditLogService.logAction(currentUser, "RESTORE", "products",
                    String.valueOf(updatedProduct.getId()),
                    "Khôi phục sản phẩm: " + updatedProduct.getName());
        } else {
            auditLogService.logAction(currentUser, "UPDATE", "products",
                    String.valueOf(updatedProduct.getId()),
                    "Cập nhật thông tin sản phẩm: " + updatedProduct.getName());
        }

        return new ProductResponse(updatedProduct);
    }

    /**
     * Xóa sản phẩm khỏi hệ thống.
     */
    @Override
    public void deleteProduct(@NonNull Integer id, User currentUser) {
        checkPermission(currentUser);


        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        if (product != null) {
            // Thực hiện Soft Delete thay vì xóa cứng (Hard Delete)
            product.setIsDeleted(true);
            productRepository.save(product);

            // Ghi Nhật ký
            auditLogService.logAction(currentUser, "DELETE", "products",
                    String.valueOf(product.getId()),
                    "Xóa sản phẩm: " + product.getName());
        }
    }

    private void deletePhysicalImage(String imageUrl) {
        if (imageUrl != null && imageUrl.contains("/uploads/images/")) {
            try {
                String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get("uploads/images", filename);
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                System.err.println("Could not delete image file: " + imageUrl);
            }
        }
    }

    @Override
    public byte[] generateExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sản phẩm");

            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Tên sản phẩm", "Tên Danh mục (Bấm mũi tên để chọn 👇)", "Giá nhập", "Giá bán", "Đơn vị tính"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                
                // Căn chỉnh độ rộng từng cột cho chuẩn xác nhất
                if (i == 1) {
                    sheet.setColumnWidth(i, 13000); // Cột Danh mục cực rộng để chứa đủ dòng text hướng dẫn và icon
                } else if (i == 0) {
                    sheet.setColumnWidth(i, 10000); // Cột Tên sản phẩm rộng rãi
                } else {
                    sheet.setColumnWidth(i, 6000);  // Các cột số liệu (Giá, Đơn vị) gọn gàng
                }
            }

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            for (int r = 1; r <= 100; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < columns.length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellStyle(dataStyle);
                }
            }

            List<Category> categories = categoryRepository.findAll();
            if (!categories.isEmpty()) {
                Sheet hiddenSheet = workbook.createSheet("Hidden_Data");
                for (int i = 0; i < categories.size(); i++) {
                    Row row = hiddenSheet.createRow(i);
                    Cell cell = row.createCell(0);
                    cell.setCellValue(categories.get(i).getName());
                }
                
                int hiddenSheetIndex = workbook.getSheetIndex(hiddenSheet);
                workbook.setSheetHidden(hiddenSheetIndex, true);

                Name namedRange = workbook.createName();
                namedRange.setNameName("CategoryList");
                namedRange.setRefersToFormula("Hidden_Data!$A$1:$A$" + categories.size());

                DataValidationHelper validationHelper = sheet.getDataValidationHelper();
                DataValidationConstraint constraint = validationHelper.createFormulaListConstraint("CategoryList");
                CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 1, 1);
                DataValidation validation = validationHelper.createValidation(constraint, addressList);
                validation.setShowErrorBox(true);
                sheet.addValidationData(validation);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo file Excel mẫu: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> importProductsFromExcel(MultipartFile file, boolean preview, User currentUser) {
        checkPermission(currentUser);

        int newCount = 0;
        int updateCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> updateDetails = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Map header để tìm đúng cột
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("File Excel không có dòng tiêu đề.");
            }

            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                headerMap.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }

            // Hỗ trợ cả tên cột cũ và mới cho Danh mục để đảm bảo tương thích ngược
            String catColName = headerMap.containsKey("Tên Danh mục (Bấm mũi tên để chọn 👇)") ? "Tên Danh mục (Bấm mũi tên để chọn 👇)" : "Tên Danh mục";

            // Kiểm tra các cột bắt buộc
            if (!headerMap.containsKey("Tên sản phẩm") || !headerMap.containsKey(catColName) || !headerMap.containsKey("Giá nhập") || !headerMap.containsKey("Giá bán")) {
                throw new RuntimeException("File Excel thiếu các cột bắt buộc (Tên sản phẩm, Tên Danh mục, Giá nhập, Giá bán).");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Kiểm tra xem dòng có trống hoàn toàn không (dựa trên các cột đã map)
                boolean isRowEmpty = true;
                for (Integer colIndex : headerMap.values()) {
                    if (!getCellValueAsString(row.getCell(colIndex)).trim().isEmpty()) {
                        isRowEmpty = false;
                        break;
                    }
                }
                // Nếu dòng trống hoàn toàn (chỉ có viền hoặc không gõ gì), bỏ qua trong im lặng
                if (isRowEmpty) continue;

                try {
                    String name = getCellValueAsString(row.getCell(headerMap.get("Tên sản phẩm")));
                    if (name.isEmpty()) throw new RuntimeException("Tên sản phẩm không được để trống.");

                    String categoryName = getCellValueAsString(row.getCell(headerMap.get(catColName)));
                    if (categoryName.isEmpty()) throw new RuntimeException("Tên Danh mục không được để trống.");

                    Category category = categoryRepository.findFirstByNameIgnoreCase(categoryName)
                            .orElseThrow(() -> new RuntimeException("Danh mục '" + categoryName + "' không tồn tại trong hệ thống."));

                    String sku = "";
                    if (headerMap.containsKey("Mã SKU")) {
                        sku = getCellValueAsString(row.getCell(headerMap.get("Mã SKU")));
                    }

                    BigDecimal importPrice = BigDecimal.ZERO;
                    String impPriceStr = getCellValueAsString(row.getCell(headerMap.get("Giá nhập")));
                    if (impPriceStr.isEmpty()) {
                        throw new RuntimeException("Giá nhập không được để trống.");
                    }
                    try { importPrice = new BigDecimal(impPriceStr); } 
                    catch (Exception e) { throw new RuntimeException("Giá nhập không hợp lệ."); }

                    BigDecimal price = BigDecimal.ZERO;
                    String priceStr = getCellValueAsString(row.getCell(headerMap.get("Giá bán")));
                    if (!priceStr.isEmpty()) {
                        try { price = new BigDecimal(priceStr); } 
                        catch (Exception e) { throw new RuntimeException("Giá bán không hợp lệ."); }
                    }

                    String unit = "Chiếc";
                    if (headerMap.containsKey("Đơn vị tính")) {
                        String u = getCellValueAsString(row.getCell(headerMap.get("Đơn vị tính")));
                        if (!u.isEmpty()) unit = u;
                    }

                    String imageUrl = "";
                    if (headerMap.containsKey("Đường dẫn ảnh (URL)")) {
                        imageUrl = getCellValueAsString(row.getCell(headerMap.get("Đường dẫn ảnh (URL)")));
                    }

                    // Xử lý Upsert dựa trên Tên sản phẩm
                    java.util.Optional<Product> existingOpt = productRepository.findFirstByNameAndIsDeletedFalse(name);
                    
                    if (existingOpt.isPresent()) {
                        Product existing = existingOpt.get();
                        
                        // Nếu có SKU truyền vào nhưng khác với SKU hiện tại -> chặn
                        if (!sku.isEmpty() && !existing.getSku().equals(sku)) {
                            // Kiểm tra xem SKU mới này đã có ai dùng chưa
                            Product skuOwner = productRepository.findBySku(sku);
                            if (skuOwner != null && !skuOwner.getId().equals(existing.getId())) {
                                throw new RuntimeException("Mã SKU '" + sku + "' đã tồn tại trong hệ thống cho một sản phẩm khác.");
                            }
                        }

                        // So sánh dữ liệu
                        boolean isChanged = false;
                        List<String> changes = new ArrayList<>();
                        java.text.NumberFormat format = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
                        
                        if (existing.getImportPrice().compareTo(importPrice) != 0) {
                            changes.add("Giá nhập (" + format.format(existing.getImportPrice()) + " đ ➔ " + format.format(importPrice) + " đ)");
                            isChanged = true;
                        }
                        if (existing.getPrice().compareTo(price) != 0) {
                            changes.add("Giá bán (" + format.format(existing.getPrice()) + " đ ➔ " + format.format(price) + " đ)");
                            isChanged = true;
                        }
                        if (!existing.getCategory().getId().equals(category.getId())) {
                            changes.add("Danh mục (" + existing.getCategory().getName() + " ➔ " + category.getName() + ")");
                            isChanged = true;
                        }
                        
                        if (isChanged) {
                            updateCount++;
                            Map<String, Object> detailMap = new HashMap<>();
                            detailMap.put("name", name);
                            detailMap.put("changes", changes);
                            updateDetails.add(detailMap);
                            
                            if (!preview) {
                                if (!sku.isEmpty()) existing.setSku(sku);
                                existing.setCategory(category);
                                existing.setImportPrice(importPrice);
                                existing.setPrice(price);
                                existing.setUnit(unit);
                                if (!imageUrl.isEmpty()) existing.setImageUrl(imageUrl);
                                productRepository.save(existing);
                            }
                        } else {
                            skippedCount++;
                        }
                        
                    } else {
                        // Thêm mới
                        if (!sku.isEmpty() && productRepository.findBySku(sku) != null) {
                             throw new RuntimeException("Mã SKU '" + sku + "' đã tồn tại trong hệ thống.");
                        }
                        
                        newCount++;
                        if (!preview) {
                            Product product = new Product();
                            if (sku.isEmpty()) {
                                product.setSku("PRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                            } else {
                                product.setSku(sku);
                            }
                            product.setName(name);
                            product.setCategory(category);
                            product.setImportPrice(importPrice);
                            product.setPrice(price);
                            product.setUnit(unit);
                            product.setImageUrl(imageUrl.isEmpty() ? null : imageUrl);
                            product.setHasExpiry(false);
                            product.setManufacturingDate(null);
                            product.setExpirationDate(null);

                            productRepository.save(product);
                        }
                    }

                } catch (Exception e) {
                    errors.add("Dòng " + (i + 1) + ": " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi đọc file Excel: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("newCount", newCount);
        result.put("updateCount", updateCount);
        result.put("skippedCount", skippedCount);
        result.put("errors", errors);
        result.put("updateDetails", updateDetails);
        // Giữ lại successCount cho tương thích ngược (hoặc tổng của new + update)
        result.put("successCount", newCount + updateCount);

        // Ghi nhật ký hệ thống nếu import THẬT và có thành công ít nhất 1 sản phẩm
        if (!preview && (newCount > 0 || updateCount > 0)) {
            String logDetails = String.format("Đã import Excel thành công. Thêm mới: %d, Cập nhật: %d. (Bỏ qua %d lỗi, %d không đổi)", 
                                              newCount, updateCount, errors.size(), skippedCount);
            auditLogService.logAction(currentUser, "IMPORT_EXCEL", "products", "Bulk", logDetails);
        }

        return result;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
                // Convert double to string without scientific notation
                return BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}
