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
    private final CustomerRepository customerRepository;

    public ReceiptServiceImpl(ReceiptRepository receiptRepository,
                              ProductRepository productRepository,
                              BranchRepository branchRepository,
                              InventoryRepository inventoryRepository,
                              CustomerRepository customerRepository) {
        this.receiptRepository = receiptRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.inventoryRepository = inventoryRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public List<ReceiptResponse> getAllReceipts(User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return receiptRepository.findAll().stream()
                    .filter(r -> !(r.getStatus() == ReceiptStatus.DRAFT && r.getCreatedBy() != null && r.getCreatedBy().getRole() == UserRole.STAFF))
                    .map(ReceiptResponse::new)
                    .collect(Collectors.toList());
        }
        
        Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
        if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");

        // MANAGER / STAFF can see receipts related to their branch
        List<Receipt> all = receiptRepository.findAll();
        return all.stream()
                .filter(r -> {
                    boolean relatedToBranch = (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) ||
                                              (r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId));
                    if (!relatedToBranch) return false;
                    
                    if (r.getStatus() == ReceiptStatus.DRAFT && r.getCreatedBy() != null && r.getCreatedBy().getRole() == UserRole.STAFF) {
                        if (currentUser.getRole() == UserRole.STAFF) {
                            return r.getCreatedBy().getId().equals(currentUser.getId());
                        }
                        // MANAGER can see it
                    }
                    return true;
                })
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
        r.setStatus(ReceiptStatus.DRAFT);
        r.setPaymentStatus(request.getPaymentStatus() == null ? "UNPAID" : request.getPaymentStatus());
        r.setCreatedBy(currentUser);
        
        if (request.getType() == ReceiptType.EXPORT) {
            if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
                throw new RuntimeException("Tên khách hàng là bắt buộc đối với phiếu xuất bán.");
            }
            if (request.getCustomerPhone() == null || request.getCustomerPhone().trim().isEmpty()) {
                throw new RuntimeException("Số điện thoại khách hàng là bắt buộc đối với phiếu xuất bán.");
            }
        }

        if (request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty()) {
            r.setCustomerName(request.getCustomerName().trim());
        }
        if (request.getCustomerPhone() != null && !request.getCustomerPhone().trim().isEmpty()) {
            r.setCustomerPhone(request.getCustomerPhone().trim());
        }

        r.setDescription(request.getDescription());

        if (request.getSourceBranchId() != null) {
            r.setSourceBranch(branchRepository.findById(request.getSourceBranchId()).orElseThrow(() -> new RuntimeException("Source branch not found")));
        }
        if (request.getDestBranchId() != null) {
            r.setDestBranch(branchRepository.findById(request.getDestBranchId()).orElseThrow(() -> new RuntimeException("Dest branch not found")));
        }

        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");

            if (request.getType() == ReceiptType.IMPORT || request.getType() == ReceiptType.ADJUST_IN) {
                if (request.getDestBranchId() == null || !request.getDestBranchId().equals(myBranchId)) {
                    throw new RuntimeException("Bạn chỉ được phép lập phiếu cho chi nhánh đích là chi nhánh của mình.");
                }
            } else {
                if (request.getSourceBranchId() == null || !request.getSourceBranchId().equals(myBranchId)) {
                    throw new RuntimeException("Bạn chỉ được phép lập phiếu xuất/điều chuyển/giảm từ kho của chi nhánh mình.");
                }
            }
        }

        if ((request.getType() == ReceiptType.IMPORT || request.getType() == ReceiptType.ADJUST_IN) && request.getDestBranchId() == null) {
             throw new RuntimeException("Chi nhánh đích là bắt buộc.");
        }
        if ((request.getType() == ReceiptType.EXPORT || request.getType() == ReceiptType.TRANSFER || request.getType() == ReceiptType.ADJUST_OUT) && request.getSourceBranchId() == null) {
             throw new RuntimeException("Kho xuất/nguồn là bắt buộc.");
        }
        if (request.getType() == ReceiptType.TRANSFER && request.getDestBranchId() == null) {
             throw new RuntimeException("Chi nhánh nhận là bắt buộc đối với phiếu điều chuyển.");
        }
        if (request.getType() == ReceiptType.TRANSFER && request.getSourceBranchId().equals(request.getDestBranchId())) {
             throw new RuntimeException("Kho xuất và nhận phải khác nhau.");
        }


        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new RuntimeException("Receipt must have details.");
        }

        if (request.getType() == ReceiptType.EXPORT || request.getType() == ReceiptType.TRANSFER || request.getType() == ReceiptType.ADJUST_OUT) {
            for (ReceiptDetailSaveRequest dReq : request.getDetails()) {
                List<Inventory> invs = inventoryRepository.findByBranchIdAndProductId(request.getSourceBranchId(), dReq.getProductId());
                int totalQty = invs.stream().mapToInt(Inventory::getQuantity).sum();
                if (totalQty < dReq.getQuantity()) {
                    Product p = productRepository.findById(dReq.getProductId()).orElse(null);
                    String pName = p != null ? p.getName() : "ID " + dReq.getProductId();
                    throw new RuntimeException("Sản phẩm " + pName + " chỉ còn " + totalQty + " trong kho nguồn, không đủ để lập phiếu.");
                }
            }
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
        }

        receiptRepository.save(r);
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
            
            if (r.getType() == ReceiptType.IMPORT || r.getType() == ReceiptType.ADJUST_IN) {
                if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Bạn không có quyền hủy phiếu của chi nhánh khác.");
                }
            } else {
                if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Bạn không có quyền hủy phiếu của chi nhánh khác.");
                }
            }
            if (currentUser.getRole() == UserRole.STAFF) {
                throw new RuntimeException("Nhân viên không có quyền hủy phiếu.");
            }
        }

        boolean wasCompleted = (r.getStatus() == ReceiptStatus.COMPLETED);
        r.setStatus(ReceiptStatus.CANCELLED);
        if (wasCompleted) {
            for (ReceiptDetail d : r.getDetails()) {
                updateInventory(r.getType(), r.getSourceBranch(), r.getDestBranch(), d, true);
            }
            updateCustomerDebt(r, true, true, false);
        }
        
        receiptRepository.save(r);
        return new ReceiptResponse(r);
    }

    private void updateInventory(ReceiptType type, Branch sourceBranch, Branch destBranch, ReceiptDetail detail, boolean isRevert) {
        int qty = detail.getQuantity();
        if (isRevert) qty = -qty; // Revert meaning opposite action

        switch (type) {
            case IMPORT:
            case ADJUST_IN:
                // Increase at Dest (IMPORT usually has destBranch = 1, ADJUST_IN has sourceBranch)
                Branch targetBranch = type == ReceiptType.IMPORT ? destBranch : sourceBranch;
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

    private void updateCustomerDebt(Receipt r, boolean isApprove, boolean isRevert, boolean isPayment) {
        if (r.getCustomerId() == null) return;
        if (r.getType() != ReceiptType.EXPORT && r.getType() != ReceiptType.IMPORT) return;
        
        java.math.BigDecimal totalAmount = r.getDetails().stream()
                .map(d -> d.getPrice().multiply(java.math.BigDecimal.valueOf(d.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                
        java.math.BigDecimal delta = java.math.BigDecimal.ZERO;
        if (isPayment) {
            if (r.getType() == ReceiptType.EXPORT) delta = totalAmount.negate();
            if (r.getType() == ReceiptType.IMPORT) delta = totalAmount;
        } else {
            if (r.getType() == ReceiptType.EXPORT) {
                delta = isRevert ? totalAmount.negate() : totalAmount;
            } else if (r.getType() == ReceiptType.IMPORT) {
                delta = isRevert ? totalAmount : totalAmount.negate();
            }
        }
        
        Customer customer = customerRepository.findById(r.getCustomerId()).orElse(null);
        if (customer != null) {
            customer.setDebt(customer.getDebt().add(delta));
            customerRepository.save(customer);
        }
    }

    private void addInventory(Branch branch, ReceiptDetail detail, int qtyDelta) {
        if (branch == null) throw new RuntimeException("Branch is required for inventory update.");
        if (qtyDelta == 0) return;

        List<Inventory> invs = inventoryRepository.findByBranchIdAndProductId(branch.getId(), detail.getProduct().getId());
        
        if (qtyDelta > 0) {
            Inventory inv;
            if (invs.isEmpty()) {
                inv = new Inventory();
                inv.setBranch(branch);
                inv.setProduct(detail.getProduct());
                inv.setQuantity(qtyDelta);
                inv.setManufacturingDate(detail.getManufacturingDate());
                inv.setExpirationDate(detail.getExpirationDate());
            } else {
                inv = invs.get(0);
                inv.setQuantity(inv.getQuantity() + qtyDelta);
            }
            inv.setLastUpdated(java.time.LocalDateTime.now());
            inventoryRepository.save(inv);
        } else {
            int remainingToDeduct = -qtyDelta;
            int totalAvailable = invs.stream().mapToInt(Inventory::getQuantity).sum();
            
            if (totalAvailable < remainingToDeduct) {
                throw new RuntimeException("Không đủ tồn kho để thực hiện giao dịch cho sản phẩm: " + detail.getProduct().getName());
            }
            
            for (Inventory inv : invs) {
                if (remainingToDeduct <= 0) break;
                if (inv.getQuantity() > 0) {
                    int deductAmount = Math.min(inv.getQuantity(), remainingToDeduct);
                    inv.setQuantity(inv.getQuantity() - deductAmount);
                    inv.setLastUpdated(java.time.LocalDateTime.now());
                    inventoryRepository.save(inv);
                    remainingToDeduct -= deductAmount;
                }
            }
        }
    }

    @Transactional
    @Override
    public ReceiptResponse approveReceipt(Integer id, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (r.getStatus() != ReceiptStatus.DRAFT) throw new RuntimeException("Phiếu không ở trạng thái chờ duyệt.");
        
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Nhân viên không có quyền duyệt phiếu.");
        }
        
        User creator = r.getCreatedBy();
        if (creator != null) {
            if (creator.getRole() == UserRole.STAFF) {
                if (currentUser.getRole() != UserRole.MANAGER) {
                    throw new RuntimeException("Chỉ Quản lý mới được quyền duyệt phiếu của Nhân viên.");
                }
            } else if (creator.getRole() == UserRole.MANAGER) {
                if (currentUser.getRole() != UserRole.ADMIN) {
                    throw new RuntimeException("Chỉ Admin mới được quyền duyệt phiếu của Quản lý.");
                }
            }
        }

        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");

            if (r.getType() == ReceiptType.IMPORT || r.getType() == ReceiptType.ADJUST_IN) {
                if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Bạn không có quyền duyệt phiếu của chi nhánh khác.");
                }
            } else {
                if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Bạn không có quyền duyệt phiếu của chi nhánh khác.");
                }
            }
        }

        r.setStatus(ReceiptStatus.COMPLETED);

        // Tạo khách hàng nếu là xuất bán và có thông tin
        if (r.getType() == ReceiptType.EXPORT && r.getCustomerName() != null && !r.getCustomerName().trim().isEmpty()) {
            String cName = r.getCustomerName().trim();
            
            Integer branchIdToUse = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (r.getSourceBranch() != null) {
                branchIdToUse = r.getSourceBranch().getId();
            }
            final Integer finalBranchId = branchIdToUse;

            Customer customer = customerRepository.findByNameAndBranchId(cName, finalBranchId).orElseGet(() -> {
                Customer newC = new Customer();
                newC.setName(cName);
                newC.setContactInfo(r.getCustomerPhone() != null ? r.getCustomerPhone().trim() : null);
                newC.setStatus("ACTIVE");
                newC.setDebt(java.math.BigDecimal.ZERO);
                if (finalBranchId != null) {
                    newC.setBranch(branchRepository.findById(finalBranchId).orElse(null));
                }
                return customerRepository.save(newC);
            });
            
            // Cập nhật số điện thoại nếu khách hàng đã tồn tại nhưng thiếu sđt hoặc khác
            if (r.getCustomerPhone() != null && !r.getCustomerPhone().trim().isEmpty()) {
                customer.setContactInfo(r.getCustomerPhone().trim());
                customerRepository.save(customer);
            }

            r.setCustomerId(customer.getId());
        }
        if (r.getType() == ReceiptType.TRANSFER) {
            r.setPaymentStatus("IN_TRANSIT");
        }
        for (ReceiptDetail d : r.getDetails()) {
            updateInventory(r.getType(), r.getSourceBranch(), r.getDestBranch(), d, false);
        }
        updateCustomerDebt(r, true, false, false);
        
        receiptRepository.save(r);
        return new ReceiptResponse(r);
    }

    @Transactional
    @Override
    public ReceiptResponse markPaid(Integer id, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        
        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");
            
            if (r.getType() == ReceiptType.EXPORT) {
                if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Chỉ chi nhánh xuất mới được xác nhận thanh toán.");
                }
            } else if (r.getType() == ReceiptType.IMPORT) {
                if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Chỉ chi nhánh nhập mới được xác nhận thanh toán.");
                }
            }
        }

        if (!"PAID".equals(r.getPaymentStatus()) && !"Đã thanh toán".equals(r.getPaymentStatus())) {
            updateCustomerDebt(r, false, false, true);
        }
        r.setPaymentStatus("PAID");
        receiptRepository.save(r);
        return new ReceiptResponse(r);
    }

    @Transactional
    @Override
    public ReceiptResponse confirmTransfer(Integer id, java.util.Map<String, Object> payload, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        
        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");
            if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                throw new RuntimeException("Chỉ chi nhánh nhận mới được xác nhận nhận hàng.");
            }
        }

        r.setPaymentStatus("RECEIVED");
        receiptRepository.save(r);
        return new ReceiptResponse(r);
    }
}
