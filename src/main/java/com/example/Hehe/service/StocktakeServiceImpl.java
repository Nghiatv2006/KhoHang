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

    @PersistenceContext
    private EntityManager entityManager;

    public StocktakeServiceImpl(StocktakeRepository stocktakeRepository,
                                StocktakeDetailRepository stocktakeDetailRepository,
                                InventoryRepository inventoryRepository,
                                BranchRepository branchRepository,
                                ProductRepository productRepository,
                                ReceiptRepository receiptRepository) {
        this.stocktakeRepository = stocktakeRepository;
        this.stocktakeDetailRepository = stocktakeDetailRepository;
        this.inventoryRepository = inventoryRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
        this.receiptRepository = receiptRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StocktakeResponse> getAllStocktakes(User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return stocktakeRepository.findAll().stream()
                    .map(StocktakeResponse::new)
                    .collect(Collectors.toList());
        }

        if (currentUser.getBranch() == null) {
            throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");
        }
        Integer branchId = currentUser.getBranch().getId();
        return stocktakeRepository.findByBranchId(branchId).stream()
                .map(StocktakeResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StocktakeResponse getStocktakeById(Integer id, User currentUser) {
        Stocktake s = stocktakeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên kiểm kê với ID: " + id));

        if (currentUser.getRole() != UserRole.ADMIN) {
            if (currentUser.getBranch() == null || !currentUser.getBranch().getId().equals(s.getBranch().getId())) {
                throw new RuntimeException("Bạn không có quyền xem thông tin kiểm kê của chi nhánh khác.");
            }
        }
        return new StocktakeResponse(s);
    }

    @Override
    public StocktakeResponse createStocktake(StocktakeSaveRequest request, User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("ADMIN không tham gia quy trình kiểm kê.");
        }
        if (currentUser.getBranch() == null) {
            throw new RuntimeException("Bạn chưa được gán vào chi nhánh nào để kiểm kê.");
        }

        Branch branch = currentUser.getBranch();

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

        logAudit(currentUser.getId(), "CREATE_STOCKTAKE", "stocktakes", s.getId().toString(),
                "Tạo phiên kiểm kê nháp " + s.getCode() + " tại " + branch.getName());

        return new StocktakeResponse(s);
    }

    @Override
    public StocktakeResponse updateStocktake(Integer id, StocktakeSaveRequest request, User currentUser) {
        Stocktake s = stocktakeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên kiểm kê với ID: " + id));

        if (s.getStatus() != StocktakeStatus.DRAFT) {
            throw new RuntimeException("Chỉ có thể chỉnh sửa số liệu phiên kiểm kê ở trạng thái DRAFT.");
        }

        if (currentUser.getRole() != UserRole.ADMIN) {
            if (currentUser.getBranch() == null || !currentUser.getBranch().getId().equals(s.getBranch().getId())) {
                throw new RuntimeException("Bạn không có quyền chỉnh sửa phiên kiểm kê của chi nhánh này.");
            }
        }

        s.setNotes(request.getNotes());

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

                d.setActualQuantity(detailReq.getActualQuantity());
                stocktakeDetailRepository.save(d);
            }
        }

        s = stocktakeRepository.save(s);

        logAudit(currentUser.getId(), "UPDATE_STOCKTAKE", "stocktakes", s.getId().toString(),
                "Cập nhật số liệu kiểm kê cho phiên " + s.getCode());

        return new StocktakeResponse(s);
    }

    @Override
    public StocktakeResponse completeStocktake(Integer id, User currentUser) {
        Stocktake s = stocktakeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên kiểm kê với ID: " + id));

        if (s.getStatus() != StocktakeStatus.DRAFT) {
            throw new RuntimeException("Chỉ có thể hoàn tất phiên kiểm kê đang ở trạng thái DRAFT.");
        }

        if (currentUser.getRole() != UserRole.MANAGER) {
            throw new RuntimeException("Chỉ Quản lý (MANAGER) mới có quyền hoàn tất kiểm kê kho.");
        }

        if (currentUser.getBranch() == null || !currentUser.getBranch().getId().equals(s.getBranch().getId())) {
            throw new RuntimeException("Bạn không có quyền duyệt hoàn tất phiên kiểm kê của chi nhánh này.");
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
        List<StocktakeDetail> surplusList = new ArrayList<>();
        List<StocktakeDetail> deficitList = new ArrayList<>();

        for (StocktakeDetail d : s.getDetails()) {
            if (d.getActualQuantity() > d.getExpectedQuantity()) {
                surplusList.add(d);
            } else if (d.getActualQuantity() < d.getExpectedQuantity()) {
                deficitList.add(d);
            }
        }

        // 3. Generate ADJUST_IN if there are surplus items
        if (!surplusList.isEmpty()) {
            Receipt receiptIn = new Receipt();
            receiptIn.setCode("AI" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
            receiptIn.setType(ReceiptType.ADJUST_IN);
            receiptIn.setStatus(ReceiptStatus.COMPLETED);
            receiptIn.setPaymentStatus("PAID");
            receiptIn.setDestBranch(branch);
            receiptIn.setCreatedBy(currentUser);
            receiptIn.setDescription("Hệ thống tự động sinh khi duyệt kiểm kê " + s.getCode() + " (Số lượng thừa)");
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
                rd.setPrice(d.getProduct().getImportPrice());
                receiptIn.getDetails().add(rd);

                // Link to receipt
                d.setAdjustmentReceipt(receiptIn);
                stocktakeDetailRepository.save(d);

                // Update inventories
                adjustInventory(branch, d.getProduct(), d.getBatchCode(), d.getManufacturingDate(), d.getExpirationDate(), qtyDiff);
            }
            receiptRepository.save(receiptIn);
        }

        // 4. Generate ADJUST_OUT if there are deficit items
        if (!deficitList.isEmpty()) {
            Receipt receiptOut = new Receipt();
            receiptOut.setCode("AO" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
            receiptOut.setType(ReceiptType.ADJUST_OUT);
            receiptOut.setStatus(ReceiptStatus.COMPLETED);
            receiptOut.setPaymentStatus("PAID");
            receiptOut.setSourceBranch(branch);
            receiptOut.setCreatedBy(currentUser);
            receiptOut.setDescription("Hệ thống tự động sinh khi duyệt kiểm kê " + s.getCode() + " (Số lượng thiếu)");
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
                rd.setPrice(d.getProduct().getImportPrice());
                receiptOut.getDetails().add(rd);

                // Link to receipt
                d.setAdjustmentReceipt(receiptOut);
                stocktakeDetailRepository.save(d);

                // Update inventories
                adjustInventory(branch, d.getProduct(), d.getBatchCode(), d.getManufacturingDate(), d.getExpirationDate(), -qtyDiff);
            }
            receiptRepository.save(receiptOut);
        }

        // 5. Complete stocktake
        s.setStatus(StocktakeStatus.COMPLETED);
        s = stocktakeRepository.save(s);

        logAudit(currentUser.getId(), "COMPLETE_STOCKTAKE", "stocktakes", s.getId().toString(),
                "Manager duyệt hoàn tất phiên kiểm kê " + s.getCode() + ". Đã cân bằng tồn kho thực tế.");

        return new StocktakeResponse(s);
    }

    @Override
    public StocktakeResponse cancelStocktake(Integer id, User currentUser) {
        Stocktake s = stocktakeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên kiểm kê với ID: " + id));

        if (s.getStatus() != StocktakeStatus.DRAFT) {
            throw new RuntimeException("Chỉ có thể hủy phiên kiểm kê đang ở trạng thái DRAFT.");
        }

        if (currentUser.getRole() != UserRole.MANAGER) {
            throw new RuntimeException("Chỉ Quản lý (MANAGER) mới có quyền hủy bỏ phiên kiểm kê.");
        }

        if (currentUser.getBranch() == null || !currentUser.getBranch().getId().equals(s.getBranch().getId())) {
            throw new RuntimeException("Bạn không có quyền hủy phiên kiểm kê của chi nhánh này.");
        }

        s.setStatus(StocktakeStatus.CANCELLED);
        s = stocktakeRepository.save(s);

        logAudit(currentUser.getId(), "CANCEL_STOCKTAKE", "stocktakes", s.getId().toString(),
                "Hủy bỏ phiên kiểm kê " + s.getCode());

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

    private void logAudit(Integer userId, String action, String entityName, String entityId, String details) {
        try {
            entityManager.createNativeQuery(
                            "INSERT INTO audit_logs (user_id, action, entity_name, entity_id, details) VALUES (?, ?, ?, ?, ?)")
                    .setParameter(1, userId)
                    .setParameter(2, action)
                    .setParameter(3, entityName)
                    .setParameter(4, entityId)
                    .setParameter(5, details)
                    .executeUpdate();
        } catch (Exception e) {
            System.err.println("Audit Log insertion failed inside Stocktake: " + e.getMessage());
        }
    }
}
