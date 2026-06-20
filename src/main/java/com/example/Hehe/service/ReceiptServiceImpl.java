package com.example.Hehe.service;

import com.example.Hehe.dto.ReceiptResponse;
import com.example.Hehe.dto.ReceiptSaveRequest;
import com.example.Hehe.dto.ReceiptDetailSaveRequest;
import com.example.Hehe.model.*;
import com.example.Hehe.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final InventoryRepository inventoryRepository;
    private final AuditLogService auditLogService;

    public ReceiptServiceImpl(ReceiptRepository receiptRepository,
                              ProductRepository productRepository,
                              BranchRepository branchRepository,
                              InventoryRepository inventoryRepository,
                              AuditLogService auditLogService) {
        this.receiptRepository = receiptRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.inventoryRepository = inventoryRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<ReceiptResponse> getAllReceipts(User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return receiptRepository.findAll().stream().map(ReceiptResponse::new).collect(Collectors.toList());
        }
        
        Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
        if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");

        // MANAGER / STAFF can see receipts related to their branch
        List<Receipt> all = receiptRepository.findAll();
        return all.stream()
                .filter(r -> (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) ||
                             (r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId)))
                .map(ReceiptResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public ReceiptResponse getReceiptById(Integer id, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        // Check permission
        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");
            
            boolean hasPerm = (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) ||
                              (r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId));
            if (!hasPerm) throw new RuntimeException("Bạn không có quyền xem phiếu này.");
        }
        return new ReceiptResponse(r);
    }

    @Transactional
    @Override
    public ReceiptResponse createReceipt(ReceiptSaveRequest request, User currentUser) {
        Receipt r = new Receipt();
        r.setCode("REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        r.setType(request.getType());
        r.setStatus(ReceiptStatus.COMPLETED);
        r.setPaymentStatus(request.getPaymentStatus() == null ? "UNPAID" : request.getPaymentStatus());
        r.setCreatedBy(currentUser);
        r.setCustomerId(request.getCustomerId());
        r.setDescription(request.getDescription());

        if (request.getSourceBranchId() != null) {
            r.setSourceBranch(branchRepository.findById(request.getSourceBranchId()).orElseThrow(() -> new RuntimeException("Source branch not found")));
        }
        if (request.getDestBranchId() != null) {
            r.setDestBranch(branchRepository.findById(request.getDestBranchId()).orElseThrow(() -> new RuntimeException("Dest branch not found")));
        }


        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new RuntimeException("Receipt must have details.");
        }

        for (ReceiptDetailSaveRequest dReq : request.getDetails()) {
            ReceiptDetail d = new ReceiptDetail();
            d.setReceipt(r);
            Product p = productRepository.findById(dReq.getProductId()).orElseThrow(() -> new RuntimeException("Product not found"));
            d.setProduct(p);
            d.setManufacturingDate(dReq.getManufacturingDate());
            d.setExpirationDate(dReq.getExpirationDate());
            d.setQuantity(dReq.getQuantity());
            d.setPrice(dReq.getPrice());
            r.getDetails().add(d);

            // Update Inventory
            updateInventory(r.getType(), r.getSourceBranch(), r.getDestBranch(), d, false);
        }

        receiptRepository.save(r);

        // Ghi Nhật ký
        String typeLabel = r.getType() != null ? r.getType().name() : "PHᯪU";
        auditLogService.logAction(currentUser, "CREATE", "receipts",
                String.valueOf(r.getId()),
                "Tạo phiếu " + typeLabel + ": " + r.getCode());

        return new ReceiptResponse(r);
    }

    @Transactional
    @Override
    public ReceiptResponse cancelReceipt(Integer id, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (r.getStatus() == ReceiptStatus.CANCELLED) throw new RuntimeException("Receipt is already cancelled.");
        
        // ADMIN can cancel any, MANAGER can cancel their own branch's
        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");
            boolean hasPerm = (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) ||
                              (r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId));
            if (!hasPerm || currentUser.getRole() == UserRole.STAFF) {
                throw new RuntimeException("Bạn không có quyền hủy phiếu này.");
            }
        }

        r.setStatus(ReceiptStatus.CANCELLED);
        for (ReceiptDetail d : r.getDetails()) {
            updateInventory(r.getType(), r.getSourceBranch(), r.getDestBranch(), d, true);
        }
        
        receiptRepository.save(r);

        // Ghi Nhật ký
        auditLogService.logAction(currentUser, "CANCEL", "receipts",
                String.valueOf(r.getId()),
                "Hủy phiếu: " + r.getCode());

        return new ReceiptResponse(r);
    }

    private void updateInventory(ReceiptType type, Branch sourceBranch, Branch destBranch, ReceiptDetail detail, boolean isRevert) {
        int qty = detail.getQuantity();
        if (isRevert) qty = -qty; // Revert meaning opposite action

        switch (type) {
            case IMPORT:
            case ADJUST_IN:
                // Increase at Dest (IMPORT usually has destBranch = 1, ADJUST_IN has destBranch)
                Branch targetBranch = (type == ReceiptType.IMPORT || type == ReceiptType.ADJUST_IN) ? destBranch : sourceBranch;
                addInventory(targetBranch, detail, qty);
                break;
            case EXPORT:
            case ADJUST_OUT:
                // Decrease at Source
                addInventory(sourceBranch, detail, -qty);
                break;
            case TRANSFER:
                // Decrease at Source, Increase at Dest
                addInventory(sourceBranch, detail, -qty);
                addInventory(destBranch, detail, qty);
                break;
        }
    }

    private void addInventory(Branch branch, ReceiptDetail detail, int qtyDelta) {
        if (branch == null) throw new RuntimeException("Branch is required for inventory update.");
        java.util.Optional<Inventory> opt = inventoryRepository.findByBranchIdAndProductIdAndBatchCode(
                branch.getId(), detail.getProduct().getId(), detail.getBatchCode());
        Inventory inv;
        if (opt.isEmpty()) {
            if (qtyDelta < 0) throw new RuntimeException("Không đủ tồn kho để thực hiện giao dịch cho sản phẩm: " + detail.getProduct().getName());
            inv = new Inventory();
            inv.setBranch(branch);
            inv.setProduct(detail.getProduct());
            inv.setQuantity(qtyDelta);
            inv.setManufacturingDate(detail.getManufacturingDate());
            inv.setExpirationDate(detail.getExpirationDate());
            inv.setBatchCode(detail.getBatchCode());
            inv.setHasExpiry(detail.getProduct().getHasExpiry());
            inv.setExpiryWarningDays(30);
        } else {
            inv = opt.get();
            int newQty = inv.getQuantity() + qtyDelta;
            if (newQty < 0) throw new RuntimeException("Không đủ tồn kho để thực hiện giao dịch cho sản phẩm: " + detail.getProduct().getName());
            inv.setQuantity(newQty);
        }
        inv.setLastUpdated(java.time.LocalDateTime.now());
        inventoryRepository.save(inv);
    }
}
