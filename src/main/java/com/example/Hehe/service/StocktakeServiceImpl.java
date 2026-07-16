package com.example.Hehe.service;

import com.example.Hehe.dto.*;
import com.example.Hehe.model.*;
import com.example.Hehe.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class StocktakeServiceImpl implements StocktakeService {

    private final StocktakeRepository stocktakeRepository;
    private final StocktakeDetailRepository stocktakeDetailRepository;
    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final ReceiptRepository receiptRepository;
    private final AuditLogService auditLogService;

    public StocktakeServiceImpl(StocktakeRepository stocktakeRepository,
                                StocktakeDetailRepository stocktakeDetailRepository,
                                InventoryRepository inventoryRepository,
                                BranchRepository branchRepository,
                                ProductRepository productRepository,
                                ReceiptRepository receiptRepository,
                                AuditLogService auditLogService) {
        this.stocktakeRepository = stocktakeRepository;
        this.stocktakeDetailRepository = stocktakeDetailRepository;
        this.inventoryRepository = inventoryRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
        this.receiptRepository = receiptRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StocktakeResponse> getAllStocktakes(User currentUser) {
        Integer branchId = getUserBranchId(currentUser);
        return stocktakeRepository.findByBranchIdOrderByCreatedAtDesc(branchId).stream()
                .filter(s -> {
                    if ("STAFF".equals(currentUser.getRole().name())) {
                        return s.getCreatedBy() != null && s.getCreatedBy().getId().equals(currentUser.getId());
                    }
                    return true;
                })
                .map(StocktakeResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StocktakeResponse getStocktakeById(Integer id, User currentUser) {
        Stocktake s = stocktakeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên kiểm kê với ID: " + id));

        Integer branchId = getUserBranchId(currentUser);
        if (!s.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Bạn không có quyền xem thông tin kiểm kê của chi nhánh khác.");
        }
        
        if ("STAFF".equals(currentUser.getRole().name())) {
            if (s.getCreatedBy() == null || !s.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Bạn chỉ được xem phiếu kiểm kê do chính mình tạo.");
            }
        }
        return new StocktakeResponse(s);
    }

    @Override
    public StocktakeResponse createStocktake(StocktakeSaveRequest request, User currentUser) {
        if (!"STAFF".equals(currentUser.getRole().name())) {
            throw new RuntimeException("Chỉ nhân viên kho (STAFF) mới có quyền khởi tạo đợt kiểm kê.");
        }

        Integer branchId = getUserBranchId(currentUser);
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh với ID: " + branchId));

        Stocktake s = new Stocktake();
        s.setCode("ST" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        s.setBranch(branch);
        s.setCreatedBy(currentUser);
        s.setStatus(StocktakeStatus.DRAFT);
        s.setNotes(request.getNotes());
        s = stocktakeRepository.save(s);

        // Fetch all inventories of the branch to auto-populate the details
        List<Inventory> inventories = inventoryRepository.findByBranchId(branch.getId());
        List<StocktakeDetail> details = new ArrayList<>();
        for (Inventory inv : inventories) {
            StocktakeDetail detail = new StocktakeDetail();
            detail.setStocktake(s);
            detail.setProduct(inv.getProduct());
            detail.setManufacturingDate(inv.getManufacturingDate());
            detail.setExpirationDate(inv.getExpirationDate());
            detail.setBatchCode(inv.getBatchCode());
            detail.setExpectedQuantity(inv.getQuantity());
            detail.setActualQuantity(inv.getQuantity()); // Default actual to expected for convenience
            details.add(detail);
        }

        if (!details.isEmpty()) {
            stocktakeDetailRepository.saveAll(details);
            s.setDetails(details);
        }

        // Save and reload
        s = stocktakeRepository.save(s);

        logAudit(currentUser, "CREATE_STOCKTAKE", "stocktakes", s.getId().toString(),
                "Tạo phiên kiểm kê nháp " + s.getCode() + " tại " + branch.getName());

        return new StocktakeResponse(s);
    }

    @Override
    public StocktakeResponse updateStocktake(Integer id, StocktakeSaveRequest request, User currentUser) {
        Stocktake s = stocktakeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên kiểm kê với ID: " + id));

        if (s.getStatus() != StocktakeStatus.DRAFT && s.getStatus() != StocktakeStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Chỉ có thể chỉnh sửa số liệu phiên kiểm kê ở trạng thái DRAFT hoặc PENDING_APPROVAL.");
        }

        if (s.getStatus() == StocktakeStatus.PENDING_APPROVAL) {
            if (currentUser.getRole() != UserRole.MANAGER && currentUser.getRole() != UserRole.ADMIN) {
                throw new RuntimeException("Chỉ Quản lý hoặc Admin mới có quyền chỉnh sửa số liệu khi chờ duyệt.");
            }
        }

        Integer branchId = getUserBranchId(currentUser);
        if (!s.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa phiên kiểm kê của chi nhánh này.");
        }

        if ("STAFF".equals(currentUser.getRole().name())) {
            if (s.getCreatedBy() == null || !s.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Bạn chỉ được chỉnh sửa phiếu kiểm kê do chính mình tạo.");
            }
        }

        s.setNotes(request.getNotes());

        List<String> changes = new ArrayList<>();

        if (request.getDetails() != null) {
            for (StocktakeDetailSaveRequest detailReq : request.getDetails()) {
                StocktakeDetail d = stocktakeDetailRepository.findById(detailReq.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy dòng chi tiết kiểm kê với ID: " + detailReq.getId()));

                if (!d.getStocktake().getId().equals(s.getId())) {
                    throw new RuntimeException("Dòng chi tiết không thuộc phiên kiểm kê hiện tại.");
                }

                if (detailReq.getActualQuantity() == null || detailReq.getActualQuantity() < 0) {
                    throw new RuntimeException("Số lượng thực tế kiểm đếm không được âm.");
                }

                if (!d.getActualQuantity().equals(detailReq.getActualQuantity())) {
                    changes.add(String.format("• %s (Lô: %s): %d -> %d", 
                        d.getProduct().getName(), 
                        d.getBatchCode() != null ? d.getBatchCode() : "Mặc định",
                        d.getActualQuantity(), 
                        detailReq.getActualQuantity()));
                }

                d.setActualQuantity(detailReq.getActualQuantity());
                stocktakeDetailRepository.save(d);
            }
        }

        s = stocktakeRepository.save(s);

        if (!changes.isEmpty()) {
            String detailMessage = "Cập nhật số liệu kiểm kê cho phiên " + s.getCode() + "\n" + String.join("\n", changes);
            logAudit(currentUser, "UPDATE_STOCKTAKE", "stocktakes", s.getId().toString(), detailMessage);
        }

        return new StocktakeResponse(s);
    }

    @Override
    public StocktakeResponse completeStocktake(Integer id, User currentUser) {
        Stocktake s = stocktakeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên kiểm kê với ID: " + id));

        if (s.getStatus() != StocktakeStatus.DRAFT) {
            throw new RuntimeException("Chỉ có thể nộp phiên kiểm kê đang ở trạng thái DRAFT.");
        }

        Integer branchId = getUserBranchId(currentUser);
        if (!s.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Bạn không có quyền nộp phiên kiểm kê của chi nhánh này.");
        }

        if ("STAFF".equals(currentUser.getRole().name())) {
            if (s.getCreatedBy() == null || !s.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Bạn chỉ được nộp phiếu kiểm kê do chính mình tạo.");
            }
        }

        Branch branch = s.getBranch();

        // 1. Recalculate expected quantities in real time
        for (StocktakeDetail d : s.getDetails()) {
            Optional<Inventory> opt = inventoryRepository.findByBranchIdAndProductIdAndBatchCode(
                    branch.getId(), d.getProduct().getId(), d.getBatchCode());
            int latestExpected = opt.map(Inventory::getQuantity).orElse(0);
            d.setExpectedQuantity(latestExpected);
            stocktakeDetailRepository.save(d);
        }

        // 2. Identify differences
        boolean hasDeviation = false;
        for (StocktakeDetail d : s.getDetails()) {
            if (!d.getActualQuantity().equals(d.getExpectedQuantity())) {
                hasDeviation = true;
                break;
            }
        }

        if (hasDeviation) {
            s.setStatus(StocktakeStatus.PENDING_APPROVAL);
            s = stocktakeRepository.save(s);
            logAudit(currentUser, "SUBMIT_STOCKTAKE", "stocktakes", s.getId().toString(),
                    "Phiếu kiểm kê " + s.getCode() + " có chênh lệch, được chuyển sang trạng thái chờ duyệt.");
        } else {
            s.setStatus(StocktakeStatus.COMPLETED);
            s = stocktakeRepository.save(s);
            logAudit(currentUser, "COMPLETE_STOCKTAKE", "stocktakes", s.getId().toString(),
                    "Hoàn tất phiên kiểm kê " + s.getCode() + ". Số lượng thực tế khớp với hệ thống.");
        }

        return new StocktakeResponse(s);
    }

    @Override
    public StocktakeResponse approveStocktake(Integer id, com.example.Hehe.dto.StocktakeApproveRequest request, User currentUser) {
        Stocktake s = stocktakeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên kiểm kê với ID: " + id));

        if (s.getStatus() != StocktakeStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Chỉ có thể duyệt phiên kiểm kê đang ở trạng thái PENDING_APPROVAL.");
        }

        if (currentUser.getRole() != UserRole.MANAGER && currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Chỉ Quản lý hoặc Admin mới có quyền duyệt kiểm kê kho.");
        }

        Integer branchId = getUserBranchId(currentUser);
        if (!s.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Bạn không có quyền duyệt phiên kiểm kê của chi nhánh này.");
        }

        Branch branch = s.getBranch();

        // Optional: Recalculate expected quantities in real time again just in case
        for (StocktakeDetail d : s.getDetails()) {
            Optional<Inventory> opt = inventoryRepository.findByBranchIdAndProductIdAndBatchCode(
                    branch.getId(), d.getProduct().getId(), d.getBatchCode());
            int latestExpected = opt.map(Inventory::getQuantity).orElse(0);
            d.setExpectedQuantity(latestExpected);
            stocktakeDetailRepository.save(d);
        }

        // Identify differences
        List<StocktakeDetail> surplusList = new ArrayList<>();
        List<StocktakeDetail> deficitList = new ArrayList<>();

        for (StocktakeDetail d : s.getDetails()) {
            if (d.getActualQuantity() > d.getExpectedQuantity()) {
                surplusList.add(d);
            } else if (d.getActualQuantity() < d.getExpectedQuantity()) {
                deficitList.add(d);
            }
        }

        String reason = request.getReason() != null ? request.getReason() : "Không có lý do";
        String responsibleStr = request.getResponsiblePersonName() != null ? request.getResponsiblePersonName() : "Hệ thống tự xử lý";
        String additionalInfo = " | Lý do: " + reason + " | Trách nhiệm: " + responsibleStr;

        // Generate ADJUST_IN if there are surplus items
        if (!surplusList.isEmpty()) {
            Receipt receiptIn = new Receipt();
            receiptIn.setCode("AI" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
            receiptIn.setType(ReceiptType.ADJUST_IN);
            receiptIn.setStatus(ReceiptStatus.COMPLETED);
            receiptIn.setPaymentStatus("PAID");
            receiptIn.setDestBranch(branch);
            receiptIn.setCreatedBy(currentUser);
            receiptIn.setDescription("Hệ thống tự động sinh khi duyệt kiểm kê " + s.getCode() + " (Số lượng thừa)" + additionalInfo);
            receiptIn = receiptRepository.save(receiptIn);

            for (StocktakeDetail d : surplusList) {
                ReceiptDetail rd = new ReceiptDetail();
                rd.setReceipt(receiptIn);
                rd.setProduct(d.getProduct());
                rd.setManufacturingDate(d.getManufacturingDate());
                rd.setExpirationDate(d.getExpirationDate());
                rd.setBatchCode(d.getBatchCode());
                
                int qtyDiff = d.getActualQuantity() - d.getExpectedQuantity();
                rd.setQuantity(qtyDiff);
                rd.setPrice(java.math.BigDecimal.ZERO);
                receiptIn.getDetails().add(rd);

                // Link to receipt
                d.setAdjustmentReceipt(receiptIn);
                stocktakeDetailRepository.save(d);

                // Update inventories
                adjustInventory(branch, d.getProduct(), d.getBatchCode(), d.getManufacturingDate(), d.getExpirationDate(), qtyDiff);
            }
            receiptRepository.save(receiptIn);
        }

        // Generate ADJUST_OUT if there are deficit items
        if (!deficitList.isEmpty()) {
            Receipt receiptOut = new Receipt();
            receiptOut.setCode("AO" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
            receiptOut.setType(ReceiptType.ADJUST_OUT);
            receiptOut.setStatus(ReceiptStatus.COMPLETED);
            receiptOut.setPaymentStatus("PAID");
            receiptOut.setSourceBranch(branch);
            receiptOut.setCreatedBy(currentUser);
            receiptOut.setDescription("Hệ thống tự động sinh khi duyệt kiểm kê " + s.getCode() + " (Số lượng thiếu)" + additionalInfo);
            receiptOut = receiptRepository.save(receiptOut);

            for (StocktakeDetail d : deficitList) {
                ReceiptDetail rd = new ReceiptDetail();
                rd.setReceipt(receiptOut);
                rd.setProduct(d.getProduct());
                rd.setManufacturingDate(d.getManufacturingDate());
                rd.setExpirationDate(d.getExpirationDate());
                rd.setBatchCode(d.getBatchCode());
                
                int qtyDiff = d.getExpectedQuantity() - d.getActualQuantity(); // Positive qty in ReceiptDetail
                rd.setQuantity(qtyDiff);
                rd.setPrice(java.math.BigDecimal.ZERO);
                receiptOut.getDetails().add(rd);

                // Link to receipt
                d.setAdjustmentReceipt(receiptOut);
                stocktakeDetailRepository.save(d);

                // Update inventories
                adjustInventory(branch, d.getProduct(), d.getBatchCode(), d.getManufacturingDate(), d.getExpirationDate(), -qtyDiff);
            }
            receiptRepository.save(receiptOut);
        }

        // Complete stocktake
        s.setStatus(StocktakeStatus.COMPLETED);
        
        // Append reason and responsible person to notes
        String existingNotes = s.getNotes() == null ? "" : s.getNotes() + "\n";
        s.setNotes(existingNotes + "Đã duyệt: " + additionalInfo);
        
        s = stocktakeRepository.save(s);

        String roleStr = currentUser.getRole() == UserRole.ADMIN ? "Admin" : "Quản lý";
        
        logAudit(currentUser, "APPROVE_STOCKTAKE", "stocktakes", s.getId().toString(),
                roleStr + " duyệt hoàn tất phiên kiểm kê " + s.getCode() + additionalInfo);

        return new StocktakeResponse(s);
    }

    @Override
    public StocktakeResponse cancelStocktake(Integer id, User currentUser) {
        Stocktake s = stocktakeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên kiểm kê với ID: " + id));

        if (s.getStatus() != StocktakeStatus.DRAFT) {
            throw new RuntimeException("Chỉ có thể hủy phiên kiểm kê đang ở trạng thái DRAFT.");
        }

        if (currentUser.getRole() != UserRole.MANAGER && currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Chỉ Quản lý hoặc Admin mới có quyền hủy bỏ phiên kiểm kê.");
        }

        Integer branchId = getUserBranchId(currentUser);
        if (!s.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Bạn không có quyền hủy phiên kiểm kê của chi nhánh này.");
        }

        if ("STAFF".equals(currentUser.getRole().name())) {
            if (s.getCreatedBy() == null || !s.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Bạn chỉ được hủy phiếu kiểm kê do chính mình tạo.");
            }
        }

        s.setStatus(StocktakeStatus.CANCELLED);
        s = stocktakeRepository.save(s);

        logAudit(currentUser, "CANCEL_STOCKTAKE", "stocktakes", s.getId().toString(),
                "Hủy bỏ phiên kiểm kê " + s.getCode());

        return new StocktakeResponse(s);
    }

    @Override
    @Transactional
    public StocktakeResponse rejectStocktake(Integer id, User currentUser) {
        Stocktake s = stocktakeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt kiểm kê"));

        if (!List.of("ADMIN", "MANAGER").contains(currentUser.getRole().name())) {
            throw new RuntimeException("Chỉ Quản lý hoặc Admin mới có quyền yêu cầu đếm lại.");
        }

        if (s.getStatus() != StocktakeStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Chỉ có thể từ chối phiếu đang chờ duyệt.");
        }

        Integer branchId = getUserBranchId(currentUser);
        if (!s.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Bạn không có quyền quản lý chi nhánh này.");
        }

        s.setStatus(StocktakeStatus.DRAFT);
        s = stocktakeRepository.save(s);

        logAudit(currentUser, "REJECT_STOCKTAKE", "stocktakes", s.getId().toString(),
                "Yêu cầu đếm lại phiên kiểm kê " + s.getCode());

        return new StocktakeResponse(s);
    }

    private void adjustInventory(Branch branch, Product product, String batchCode,
                                 java.time.LocalDate mfgDate, java.time.LocalDate expDate, int qtyDelta) {
        Optional<Inventory> opt = inventoryRepository.findByBranchIdAndProductIdAndBatchCode(
                branch.getId(), product.getId(), batchCode);

        Inventory inv;
        if (opt.isEmpty()) {
            if (qtyDelta < 0) {
                throw new RuntimeException("Không thể giảm tồn kho cho lô hàng không tồn tại: " + product.getName());
            }
            inv = new Inventory();
            inv.setBranch(branch);
            inv.setProduct(product);
            inv.setBatchCode(batchCode);
            inv.setQuantity(qtyDelta);
            inv.setManufacturingDate(mfgDate);
            inv.setExpirationDate(expDate);
            inv.setHasExpiry(product.getHasExpiry());
            inv.setExpiryWarningDays(30);
        } else {
            inv = opt.get();
            int newQty = inv.getQuantity() + qtyDelta;
            if (newQty < 0) {
                throw new RuntimeException("Số lượng tồn kho của sản phẩm '" + product.getName() + "' không thể giảm dưới 0.");
            }
            inv.setQuantity(newQty);
        }
        inv.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(inv);
    }

    private void logAudit(User user, String action, String entityName, String entityId, String details) {
        try {
            auditLogService.logAction(user, action, entityName, entityId, details);
        } catch (Exception e) {
            System.err.println("Audit Log insertion failed inside Stocktake: " + e.getMessage());
        }
    }

    private Integer getUserBranchId(User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return currentUser.getBranch() != null ? currentUser.getBranch().getId() :
                   branchRepository.findByIsHeadTrue().stream().findFirst()
                           .orElseThrow(() -> new RuntimeException("Không tìm thấy kho tổng.")).getId();
        }
        if (currentUser.getBranch() == null) {
            throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");
        }
        return currentUser.getBranch().getId();
    }
}
