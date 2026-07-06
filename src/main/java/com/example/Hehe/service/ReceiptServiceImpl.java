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
    private final AuditLogService auditLogService;

    public ReceiptServiceImpl(ReceiptRepository receiptRepository,
                              ProductRepository productRepository,
                              BranchRepository branchRepository,
                              InventoryRepository inventoryRepository,
                              CustomerRepository customerRepository,
                              AuditLogService auditLogService) {
        this.receiptRepository = receiptRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.inventoryRepository = inventoryRepository;
        this.customerRepository = customerRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getAllReceipts(User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            return receiptRepository.findAll().stream()
                    .filter(r -> {
                        if (r.getType() == ReceiptType.IMPORT && r.getStatus() == ReceiptStatus.DRAFT) {
                            boolean isMyBranch = myBranchId != null && r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId);
                            boolean isCreator = r.getCreatedBy() != null && r.getCreatedBy().getId().equals(currentUser.getId());
                            return isMyBranch || isCreator;
                        }
                        if (r.getType() == ReceiptType.TRANSFER) {
                            if (myBranchId == null) return false;
                            return (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) ||
                                   (r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId));
                        }
                        return true;
                    })
                    .map(ReceiptResponse::new)
                    .collect(Collectors.toList());
        }
        
        Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
        if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");

        List<Receipt> all = receiptRepository.findAll();
        return all.stream()
                .filter(r -> {
                    boolean relatedToBranch = (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) ||
                                              (r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId));
                    if (!relatedToBranch) return false;
                    
                    if (currentUser.getRole() == UserRole.STAFF) {
                        if (r.getType() == ReceiptType.TRANSFER && r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) {
                            return false;
                        }
                        boolean isMyReceipt = r.getCreatedBy() != null && r.getCreatedBy().getId().equals(currentUser.getId());
                        boolean isIncoming = (r.getType() == ReceiptType.IMPORT || r.getType() == ReceiptType.TRANSFER) 
                                             && r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId)
                                             && (r.getStatus() != ReceiptStatus.DRAFT && r.getStatus() != ReceiptStatus.PENDING_ADMIN);
                        return isMyReceipt || isIncoming;
                    }
                    
                    return true;
                })
                .map(ReceiptResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptById(Integer id, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (currentUser.getRole() == UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (r.getType() == ReceiptType.IMPORT && r.getStatus() == ReceiptStatus.DRAFT) {
                boolean isMyBranch = myBranchId != null && r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId);
                boolean isCreator = r.getCreatedBy() != null && r.getCreatedBy().getId().equals(currentUser.getId());
                if (!isMyBranch && !isCreator) {
                    throw new RuntimeException("Phiếu nhập kho này chưa được quản lý chi nhánh duyệt.");
                }
            }
            if (r.getType() == ReceiptType.TRANSFER) {
                if (myBranchId == null) throw new RuntimeException("Bạn không có quyền xem phiếu này.");
                boolean hasPerm = (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) ||
                                  (r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId));
                if (!hasPerm) throw new RuntimeException("Bạn không có quyền xem phiếu này.");
            }
        }
        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");
            
            boolean hasPerm = (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) ||
                              (r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId));
            if (!hasPerm) throw new RuntimeException("Bạn không có quyền xem phiếu này.");
            
            if (currentUser.getRole() == UserRole.STAFF) {
                if (r.getType() == ReceiptType.TRANSFER && r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Bạn không có quyền xem phiếu này.");
                }
                boolean isMyReceipt = r.getCreatedBy() != null && r.getCreatedBy().getId().equals(currentUser.getId());
                boolean isIncoming = (r.getType() == ReceiptType.IMPORT || r.getType() == ReceiptType.TRANSFER) 
                                     && r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId)
                                     && (r.getStatus() != ReceiptStatus.DRAFT && r.getStatus() != ReceiptStatus.PENDING_ADMIN);
                if (!isMyReceipt && !isIncoming) {
                    throw new RuntimeException("Bạn không có quyền xem phiếu này.");
                }
            }
        }
        return new ReceiptResponse(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getReceiptsByCustomer(Integer customerId, User currentUser) {
        List<Receipt> receipts = receiptRepository.findByCustomerId(customerId);
        return receipts.stream()
                .filter(r -> {
                    if (currentUser.getRole() == UserRole.ADMIN) {
                        if (r.getType() == ReceiptType.IMPORT && r.getStatus() == ReceiptStatus.DRAFT) {
                            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
                            boolean isMyBranch = myBranchId != null && r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId);
                            boolean isCreator = r.getCreatedBy() != null && r.getCreatedBy().getId().equals(currentUser.getId());
                            return isMyBranch || isCreator;
                        }
                        return true;
                    }
                    Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
                    if (myBranchId == null) return false;
                    boolean relatedToBranch = (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) ||
                                              (r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId));
                    if (!relatedToBranch) return false;
                    
                    if (currentUser.getRole() == UserRole.STAFF) {
                        if (r.getType() == ReceiptType.TRANSFER && r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) {
                            return false;
                        }
                        boolean isMyReceipt = r.getCreatedBy() != null && r.getCreatedBy().getId().equals(currentUser.getId());
                        boolean isIncoming = (r.getType() == ReceiptType.IMPORT || r.getType() == ReceiptType.TRANSFER) 
                                             && r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId)
                                             && (r.getStatus() != ReceiptStatus.DRAFT && r.getStatus() != ReceiptStatus.PENDING_ADMIN);
                        return isMyReceipt || isIncoming;
                    }
                    
                    return true;
                })
                .map(ReceiptResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public ReceiptResponse createReceipt(ReceiptSaveRequest request, User currentUser) {
        Receipt r = new Receipt();
        r.setCode("REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        r.setType(request.getType());
        if (request.getType() == ReceiptType.DISPOSAL) {
            r.setStatus(ReceiptStatus.PENDING_ADMIN);
        } else {
            r.setStatus(ReceiptStatus.DRAFT);
        }
        r.setPaymentStatus(request.getPaymentStatus() == null ? "UNPAID" : request.getPaymentStatus());
        r.setCreatedBy(currentUser);
        
        if (request.getSourceBranchId() != null) {
            r.setSourceBranch(branchRepository.findById(request.getSourceBranchId()).orElseThrow(() -> new RuntimeException("Source branch not found")));
        } else if (request.getType() == ReceiptType.IMPORT) {
            Branch head = branchRepository.findByIsHeadTrue().stream().findFirst()
                    .orElseGet(() -> branchRepository.findById(1).orElse(null));
            r.setSourceBranch(head);
        }
        
        if (request.getDestBranchId() != null) {
            r.setDestBranch(branchRepository.findById(request.getDestBranchId()).orElseThrow(() -> new RuntimeException("Dest branch not found")));
        }

        if (request.getType() == ReceiptType.EXPORT) {
            if (request.getCustomerId() == null) {
                if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
                    throw new RuntimeException("Tên khách hàng là bắt buộc đối với phiếu xuất bán.");
                }
                if (request.getCustomerPhone() == null || request.getCustomerPhone().trim().isEmpty()) {
                    throw new RuntimeException("Số điện thoại khách hàng là bắt buộc đối với phiếu xuất bán.");
                }
            }
        }

        if (request.getCustomerId() != null) {
            r.setCustomerId(request.getCustomerId());
            Customer customer = customerRepository.findById(request.getCustomerId()).orElse(null);
            if (customer != null) {
                r.setCustomerName(customer.getName());
                r.setCustomerPhone(customer.getContactInfo());
            }
            if (request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty()) {
                r.setCustomerName(request.getCustomerName().trim());
            }
            if (request.getCustomerPhone() != null && !request.getCustomerPhone().trim().isEmpty()) {
                r.setCustomerPhone(request.getCustomerPhone().trim());
            }
        } else if (request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty()) {
            String cName = request.getCustomerName().trim();
            Branch targetBranch = (request.getType() == ReceiptType.IMPORT) ? r.getDestBranch() : r.getSourceBranch();
            if (targetBranch == null) targetBranch = currentUser.getBranch();
            
            Customer customer = null;
            if (request.getCustomerPhone() != null && !request.getCustomerPhone().trim().isEmpty()) {
                customer = customerRepository.findFirstByContactInfo(request.getCustomerPhone().trim()).orElse(null);
            }
            if (customer == null) {
                if (targetBranch != null) {
                    customer = customerRepository.findFirstByBranchIdAndName(targetBranch.getId(), cName).orElse(null);
                } else {
                    customer = customerRepository.findFirstByName(cName).orElse(null);
                }
            }
            if (customer == null) {
                Customer newC = new Customer();
                newC.setName(cName);
                newC.setContactInfo(request.getCustomerPhone() != null ? request.getCustomerPhone().trim() : null);
                newC.setStatus("ACTIVE");
                newC.setDebt(java.math.BigDecimal.ZERO);
                newC.setBranch(targetBranch);
                customer = customerRepository.save(newC);
            }
            r.setCustomerId(customer.getId());
            r.setCustomerName(customer.getName());
            r.setCustomerPhone(customer.getContactInfo());
        }

        if (request.getType() == ReceiptType.EXPORT) {
            if (r.getCustomerId() == null && (r.getCustomerName() == null || r.getCustomerName().trim().isEmpty())) {
                throw new RuntimeException("Khách hàng là bắt buộc đối với phiếu xuất bán.");
            }
        }
        r.setDescription(request.getDescription());

        // DISPOSAL-specific fields
        if (request.getType() == ReceiptType.DISPOSAL) {
            if (request.getDisposalReason() == null || request.getDisposalReason().trim().isEmpty()) {
                throw new RuntimeException("Lý do tiêu hủy là bắt buộc.");
            }
            r.setDisposalReason(request.getDisposalReason());
            r.setDisposalMethod(request.getDisposalMethod());
            r.setAttachmentUrl(request.getAttachmentUrl());
        }

        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");

            if (currentUser.getRole() == UserRole.MANAGER) {
                throw new RuntimeException("Quản lý chỉ được phép duyệt phiếu, việc lập phiếu do Nhân viên thực hiện.");
            }

            if (request.getType() == ReceiptType.IMPORT || request.getType() == ReceiptType.ADJUST_IN || request.getType() == ReceiptType.TRANSFER) {
                if (request.getDestBranchId() == null || !request.getDestBranchId().equals(myBranchId)) {
                    throw new RuntimeException("Bạn chỉ được phép lập phiếu cho chi nhánh đích là chi nhánh của mình.");
                }
            } else {
                if (request.getSourceBranchId() == null || !request.getSourceBranchId().equals(myBranchId)) {
                    throw new RuntimeException("Bạn chỉ được phép lập phiếu xuất/giảm từ kho của chi nhánh mình.");
                }
            }
        }

        if ((request.getType() == ReceiptType.IMPORT || request.getType() == ReceiptType.ADJUST_IN) && request.getDestBranchId() == null) {
             throw new RuntimeException("Chi nhánh đích là bắt buộc.");
        }
        if ((request.getType() == ReceiptType.EXPORT || request.getType() == ReceiptType.TRANSFER || request.getType() == ReceiptType.ADJUST_OUT || request.getType() == ReceiptType.DISPOSAL) && request.getSourceBranchId() == null) {
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

        if (request.getType() == ReceiptType.EXPORT || request.getType() == ReceiptType.TRANSFER || request.getType() == ReceiptType.ADJUST_OUT || request.getType() == ReceiptType.DISPOSAL || (request.getType() == ReceiptType.IMPORT && request.getSourceBranchId() != null && !request.getSourceBranchId().equals(request.getDestBranchId()))) {
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
            
            if (p.getHasExpiry() && dReq.getManufacturingDate() != null && dReq.getExpirationDate() != null) {
                if (dReq.getManufacturingDate().isAfter(dReq.getExpirationDate())) {
                    throw new RuntimeException("Hạn sử dụng không hợp lệ (Ngày sản xuất không được lớn hơn Hạn sử dụng).");
                }
            }
            
            d.setProduct(p);
            d.setBatchCode(dReq.getBatchCode() != null && !dReq.getBatchCode().isEmpty() ? dReq.getBatchCode() : "DEFAULT_BATCH");
            d.setManufacturingDate(dReq.getManufacturingDate());
            d.setExpirationDate(dReq.getExpirationDate());
            d.setQuantity(dReq.getQuantity());
            d.setPrice(dReq.getPrice());
            r.getDetails().add(d);
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
        if (r.getStatus() == ReceiptStatus.PENDING_SHORTFALL_ADMIN) throw new RuntimeException("Phiếu đã chuyển lên Admin duyệt thiếu hụt thì không thể hủy.");
        
        if (currentUser.getRole() == UserRole.ADMIN) {
            if (r.getType() == ReceiptType.EXPORT && r.getSourceBranch() != null && r.getSourceBranch().getId() != 1) {
                throw new RuntimeException("Admin chỉ quản lý phiếu xuất bán của Chi nhánh tổng.");
            }
        }
        
        // ADMIN can cancel any, MANAGER can cancel their own branch's
        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");
            if (r.getType() == ReceiptType.IMPORT || r.getType() == ReceiptType.ADJUST_IN) {
                boolean isCrossBranch = (r.getType() == ReceiptType.IMPORT && r.getSourceBranch() != null && r.getDestBranch() != null && !r.getSourceBranch().getId().equals(r.getDestBranch().getId()));
                if (isCrossBranch) {
                    if (!r.getDestBranch().getId().equals(myBranchId) && !r.getSourceBranch().getId().equals(myBranchId)) {
                        throw new RuntimeException("Bạn không có quyền hủy phiếu của chi nhánh khác.");
                    }
                } else {
                    if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                        throw new RuntimeException("Bạn không có quyền hủy phiếu của chi nhánh khác.");
                    }
                }
            } else if (r.getType() == ReceiptType.TRANSFER) {
                if ((r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) && 
                    (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId))) {
                    throw new RuntimeException("Bạn không có quyền hủy phiếu điều chuyển của chi nhánh khác.");
                }
            } else {
                // EXPORT, ADJUST_OUT, DISPOSAL
                if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Bạn không có quyền hủy phiếu của chi nhánh khác.");
                }
            }
            if (currentUser.getRole() == UserRole.STAFF) {
                throw new RuntimeException("Nhân viên không có quyền hủy phiếu.");
            }
        }

        boolean wasCompleted = (r.getStatus() == ReceiptStatus.COMPLETED);
        boolean wasDeducted = false;
        if (r.getType() == ReceiptType.TRANSFER && (r.getStatus() == ReceiptStatus.PENDING_ADMIN || r.getStatus() == ReceiptStatus.PENDING_STOCKTAKE)) {
            wasDeducted = true;
        }
        if (r.getType() == ReceiptType.IMPORT && r.getStatus() == ReceiptStatus.PENDING_STOCKTAKE) {
            wasDeducted = true;
        }

        r.setStatus(ReceiptStatus.CANCELLED);
        if (wasCompleted) {
            for (ReceiptDetail d : r.getDetails()) {
                updateInventory(r.getType(), r.getSourceBranch(), r.getDestBranch(), d, true);
                if (r.getType() == ReceiptType.TRANSFER && "RECEIVED".equals(r.getPaymentStatus())) {
                    if (d.getReceivedQuantity() != null) {
                         addInventory(r.getDestBranch(), d, -d.getReceivedQuantity());
                    }
                }
            }
            updateCustomerDebt(r, true, true, false);
        } else if (wasDeducted) {
            boolean shouldRevertSource = true;
            if (r.getType() == ReceiptType.IMPORT) {
                boolean isCrossBranch = (r.getSourceBranch() != null && r.getDestBranch() != null && !r.getSourceBranch().getId().equals(r.getDestBranch().getId()));
                if (!isCrossBranch) {
                    shouldRevertSource = false;
                }
            }
            if (shouldRevertSource) {
                for (ReceiptDetail d : r.getDetails()) {
                    addInventory(r.getSourceBranch(), d, d.getQuantity());
                }
            }
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
                addInventory(destBranch, detail, qty);
                if (sourceBranch != null && !sourceBranch.getId().equals(destBranch.getId())) {
                    addInventory(sourceBranch, detail, -qty);
                }
                break;
            case ADJUST_IN:
                // Increase at target branch
                Branch targetBranch = destBranch != null ? destBranch : sourceBranch;
                addInventory(targetBranch, detail, qty);
                break;
            case EXPORT:
            case ADJUST_OUT:
            case DISPOSAL:
                // Decrease at Source
                addInventory(sourceBranch, detail, -qty);
                break;
            case TRANSFER:
                // Decrease at Source only (will increase at Dest upon receive)
                addInventory(sourceBranch, detail, -qty);
                break;
        }
    }

    private void updateCustomerDebt(Receipt r, boolean isApprove, boolean isRevert, boolean isPayment) {
        if (r.getCustomerId() == null) return;
        if (r.getType() != ReceiptType.EXPORT && r.getType() != ReceiptType.IMPORT) return;
        
        java.math.BigDecimal totalAmount = r.getDetails().stream()
                .map(d -> {
                    int qtyForDebt = (r.getStatus() == ReceiptStatus.COMPLETED && d.getReceivedQuantity() != null) 
                                     ? d.getReceivedQuantity() 
                                     : d.getQuantity();
                    return d.getPrice().multiply(java.math.BigDecimal.valueOf(qtyForDebt));
                })
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
            java.math.BigDecimal newDebt = customer.getDebt().add(delta);
            if (newDebt.compareTo(java.math.BigDecimal.ZERO) < 0) {
                newDebt = java.math.BigDecimal.ZERO;
            }
            customer.setDebt(newDebt);
            customerRepository.save(customer);
        }
    }

    private void addInventory(Branch branch, ReceiptDetail detail, int qtyDelta) {
        if (branch == null) throw new RuntimeException("Branch is required for inventory update.");
        
        if (qtyDelta < 0) {
            int remainingToDeduct = -qtyDelta;
            java.util.List<Inventory> availableInvs = inventoryRepository.findByBranchIdAndProductId(branch.getId(), detail.getProduct().getId());
            
            availableInvs.sort((i1, i2) -> {
                if (i1.getExpirationDate() == null && i2.getExpirationDate() == null) return 0;
                if (i1.getExpirationDate() == null) return 1;
                if (i2.getExpirationDate() == null) return -1;
                return i1.getExpirationDate().compareTo(i2.getExpirationDate());
            });
            
            int totalAvailable = availableInvs.stream().mapToInt(Inventory::getQuantity).sum();
            if (totalAvailable < remainingToDeduct) {
                throw new RuntimeException("Không đủ tồn kho để thực hiện giao dịch cho sản phẩm: " + detail.getProduct().getName());
            }
            
            for (Inventory inv : availableInvs) {
                if (remainingToDeduct == 0) break;
                if (inv.getQuantity() > 0) {
                    int deduct = Math.min(inv.getQuantity(), remainingToDeduct);
                    inv.setQuantity(inv.getQuantity() - deduct);
                    inv.setLastUpdated(java.time.LocalDateTime.now());
                    inventoryRepository.save(inv);
                    remainingToDeduct -= deduct;
                }
            }
            
            if (remainingToDeduct > 0) {
                throw new RuntimeException("Không đủ tồn kho để thực hiện giao dịch cho sản phẩm: " + detail.getProduct().getName());
            }
        } else {
            java.util.Optional<Inventory> opt = inventoryRepository.findByBranchIdAndProductIdAndBatchCode(
                    branch.getId(), detail.getProduct().getId(), detail.getBatchCode());
            Inventory inv;
            if (opt.isEmpty()) {
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
                inv.setQuantity(inv.getQuantity() + qtyDelta);
                inv.setManufacturingDate(detail.getManufacturingDate());
                inv.setExpirationDate(detail.getExpirationDate());
                inv.setHasExpiry(detail.getProduct().getHasExpiry());
            }
            inv.setLastUpdated(java.time.LocalDateTime.now());
            inventoryRepository.save(inv);
        }
    }


    @Transactional
    @Override
    public ReceiptResponse approveReceipt(Integer id, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (r.getType() != ReceiptType.IMPORT && r.getType() != ReceiptType.TRANSFER && r.getType() != ReceiptType.DISPOSAL && r.getStatus() != ReceiptStatus.DRAFT) {
            throw new RuntimeException("Phiếu không ở trạng thái chờ duyệt.");
        }
        if ((r.getType() == ReceiptType.IMPORT || r.getType() == ReceiptType.TRANSFER) && r.getStatus() != ReceiptStatus.DRAFT && r.getStatus() != ReceiptStatus.PENDING_ADMIN) {
            throw new RuntimeException("Phiếu không ở trạng thái có thể duyệt.");
        }
        if (r.getType() == ReceiptType.DISPOSAL && r.getStatus() != ReceiptStatus.DRAFT && r.getStatus() != ReceiptStatus.PENDING_ADMIN && r.getStatus() != ReceiptStatus.PENDING_STOCKTAKE) {
            throw new RuntimeException("Phiếu tiêu hủy không ở trạng thái có thể duyệt.");
        }
        
        if (currentUser.getRole() == UserRole.ADMIN && r.getType() == ReceiptType.TRANSFER) {
            throw new RuntimeException("Admin không được quyền can thiệp vào phiếu điều chuyển giữa các chi nhánh.");
        }

        if (currentUser.getRole() == UserRole.STAFF) {
            if (r.getType() == ReceiptType.DISPOSAL) {
                if (r.getStatus() != ReceiptStatus.DRAFT) {
                    throw new RuntimeException("Phiếu tiêu hủy bắt buộc phải do Quản lý duyệt.");
                }
            } else if (r.getType() != ReceiptType.ADJUST_OUT && r.getType() != ReceiptType.EXPORT) {
                throw new RuntimeException("Nhân viên không có quyền duyệt phiếu này.");
            }
        }
        
        if (currentUser.getRole() == UserRole.ADMIN) {
            if (r.getType() == ReceiptType.EXPORT && r.getSourceBranch() != null && r.getSourceBranch().getId() != 1) {
                throw new RuntimeException("Admin chỉ quản lý phiếu xuất bán của Chi nhánh tổng.");
            }
        }

        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");

            if (r.getType() == ReceiptType.IMPORT) {
                boolean isCrossBranch = (r.getSourceBranch() != null && r.getDestBranch() != null && !r.getSourceBranch().getId().equals(r.getDestBranch().getId()));
                if (isCrossBranch) {
                    if (!r.getSourceBranch().getId().equals(myBranchId) && !r.getDestBranch().getId().equals(myBranchId)) {
                        throw new RuntimeException("Chỉ Quản lý chi nhánh liên quan mới có quyền duyệt phiếu nhập kho.");
                    }
                } else {
                    if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                        throw new RuntimeException("Bạn không có quyền thao tác trên phiếu của chi nhánh khác.");
                    }
                }
            } else if (r.getType() == ReceiptType.TRANSFER) {
                if (r.getStatus() == ReceiptStatus.DRAFT) {
                    if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                        throw new RuntimeException("Chỉ Quản lý chi nhánh đích mới có quyền duyệt yêu cầu xin hàng.");
                    }
                } else if (r.getStatus() == ReceiptStatus.PENDING_ADMIN) {
                    if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                        throw new RuntimeException("Chỉ Quản lý chi nhánh nguồn mới có quyền duyệt xuất kho.");
                    }
                }
            } else if (r.getType() == ReceiptType.ADJUST_IN) {
                if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Bạn không có quyền duyệt phiếu của chi nhánh khác.");
                }
            } else {
                // EXPORT, ADJUST_OUT, DISPOSAL: check source branch
                if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Bạn không có quyền duyệt phiếu của chi nhánh khác.");
                }
            }
        }

        // ── DISPOSAL: Quá trình duyệt 2 bước (Manager -> Admin) ──
        if (r.getType() == ReceiptType.DISPOSAL) {
            if (r.getStatus() == ReceiptStatus.DRAFT) {
                if (currentUser.getRole() == UserRole.MANAGER) {
                    r.setStatus(ReceiptStatus.PENDING_STOCKTAKE);
                    receiptRepository.save(r);
                    auditLogService.logAction(currentUser, "APPROVE", "receipts",
                            String.valueOf(r.getId()),
                            "Phiếu tiêu hủy " + r.getCode() + " đã được Quản lý duyệt, chờ Admin xác nhận.");
                    return new ReceiptResponse(r);
                } else if (currentUser.getRole() != UserRole.ADMIN) {
                    r.setStatus(ReceiptStatus.PENDING_ADMIN);
                    receiptRepository.save(r);
                    auditLogService.logAction(currentUser, "APPROVE", "receipts", String.valueOf(r.getId()), "Duyệt phiếu " + r.getType().name() + " " + r.getCode() + " -> " + r.getStatus().name());
                    return new ReceiptResponse(r);
                }
            } else if (r.getStatus() == ReceiptStatus.PENDING_ADMIN) {
                if (currentUser.getRole() != UserRole.MANAGER && currentUser.getRole() != UserRole.ADMIN) {
                    throw new RuntimeException("Chỉ Quản lý mới có quyền duyệt phiếu tiêu hủy.");
                }
                
                if (currentUser.getRole() == UserRole.MANAGER) {
                    r.setStatus(ReceiptStatus.PENDING_STOCKTAKE);
                    receiptRepository.save(r);
                    auditLogService.logAction(currentUser, "APPROVE", "receipts",
                            String.valueOf(r.getId()),
                            "Phiếu tiêu hủy " + r.getCode() + " đã được Quản lý duyệt, chờ Admin xác nhận.");
                    return new ReceiptResponse(r);
                }
            } else if (r.getStatus() == ReceiptStatus.PENDING_STOCKTAKE) {
                if (currentUser.getRole() != UserRole.ADMIN) {
                    throw new RuntimeException("Chỉ Admin mới có quyền duyệt cuối cùng phiếu tiêu hủy.");
                }
            }
        }

        if (r.getApprovedBy() == null || currentUser.getRole() != UserRole.ADMIN) {
            r.setApprovedBy(currentUser);
        }

        if (r.getType() == ReceiptType.IMPORT || r.getType() == ReceiptType.TRANSFER || r.getType() == ReceiptType.ADJUST_OUT) {
            if (r.getStatus() == ReceiptStatus.DRAFT) {
                // Mới DRAFT -> PENDING_ADMIN (Hoặc COMPLETED luôn với ADJUST_OUT)
                if (r.getType() == ReceiptType.ADJUST_OUT) {
                    if (currentUser.getRole() == UserRole.STAFF) {
                        java.math.BigDecimal totalValue = java.math.BigDecimal.ZERO;
                        boolean hasMilk = false;
                        for (ReceiptDetail d : r.getDetails()) {
                            totalValue = totalValue.add(d.getPrice().multiply(java.math.BigDecimal.valueOf(d.getQuantity())));
                            if (d.getProduct() != null && d.getProduct().getCategory() != null) {
                                if (d.getProduct().getCategory().getName().toLowerCase().contains("sữa")) {
                                    hasMilk = true;
                                }
                            }
                        }
                        if (!hasMilk && totalValue.compareTo(new java.math.BigDecimal("45000000")) >= 0) {
                            r.setStatus(ReceiptStatus.PENDING_ADMIN);
                            receiptRepository.save(r);
                            auditLogService.logAction(currentUser, "APPROVE", "receipts", String.valueOf(r.getId()), "Duyệt phiếu " + r.getType().name() + " " + r.getCode() + " -> " + r.getStatus().name());
                            return new ReceiptResponse(r);
                        }
                    }
                    // Nếu là Manager/Admin hoặc Staff (dưới 45tr/Sữa), phiếu ADJUST_OUT sẽ bỏ qua PENDING_ADMIN và chạy xuống để COMPLETED
                } else {
                    if (currentUser.getRole() != UserRole.MANAGER && currentUser.getRole() != UserRole.ADMIN && currentUser.getRole() != UserRole.STAFF) {
                        throw new RuntimeException("Bạn không có quyền duyệt phiếu ở bước này.");
                    }
                    if (r.getType() == ReceiptType.EXPORT) {
                        // Bỏ qua PENDING_ADMIN để chạy xuống cuối hàm và set COMPLETED
                    } else {
                        if (r.getType() == ReceiptType.TRANSFER) {
                            // Không trừ tồn kho ở bước xin hàng, chờ chi nhánh Nguồn duyệt mới trừ
                        }
                        r.setStatus(ReceiptStatus.PENDING_ADMIN);
                        receiptRepository.save(r);
                        auditLogService.logAction(currentUser, "APPROVE", "receipts", String.valueOf(r.getId()), "Duyệt phiếu " + r.getType().name() + " " + r.getCode() + " -> " + r.getStatus().name());
                        return new ReceiptResponse(r);
                    }
                }
            } else if (r.getStatus() == ReceiptStatus.PENDING_ADMIN) {
                if (r.getType() == ReceiptType.ADJUST_OUT) {
                    if (currentUser.getRole() == UserRole.STAFF) {
                        throw new RuntimeException("Chỉ Quản lý mới có quyền duyệt phiếu giảm tồn kho lớn.");
                    }
                    // Manager/Admin duyệt -> Chạy xuống dòng để COMPLETED
                } else {
                    // PENDING_ADMIN -> PENDING_STOCKTAKE cho IMPORT / TRANSFER
                    if (r.getType() == ReceiptType.IMPORT) {
                        if (currentUser.getRole() != UserRole.ADMIN) {
                            throw new RuntimeException("Chỉ Admin mới có quyền duyệt lên bước Kiểm kê.");
                        }

                        // Khi Admin duyệt, hàng hóa ở chi nhánh nguồn (kho tổng) chính thức bị trừ đi
                        boolean isCrossBranch = (r.getSourceBranch() != null && r.getDestBranch() != null && !r.getSourceBranch().getId().equals(r.getDestBranch().getId()));
                        if (isCrossBranch) {
                            for (ReceiptDetail d : r.getDetails()) {
                                addInventory(r.getSourceBranch(), d, -d.getQuantity());
                            }
                        }
                    } else if (r.getType() == ReceiptType.TRANSFER) {
                        // Manager nguồn duyệt xuất kho -> trừ tồn kho
                        for (ReceiptDetail d : r.getDetails()) {
                            addInventory(r.getSourceBranch(), d, -d.getQuantity());
                        }
                    }

                    r.setStatus(ReceiptStatus.PENDING_STOCKTAKE);
                    receiptRepository.save(r);
                    auditLogService.logAction(currentUser, "APPROVE", "receipts", String.valueOf(r.getId()), "Duyệt phiếu " + r.getType().name() + " " + r.getCode() + " -> " + r.getStatus().name());
                    return new ReceiptResponse(r);
                }
            }
        }

        r.setStatus(ReceiptStatus.COMPLETED);


        if (r.getType() == ReceiptType.TRANSFER) {
            r.setPaymentStatus("IN_TRANSIT");
        }
        for (ReceiptDetail d : r.getDetails()) {
            updateInventory(r.getType(), r.getSourceBranch(), r.getDestBranch(), d, false);
        }
        updateCustomerDebt(r, true, false, false);
        if ("PAID".equals(r.getPaymentStatus()) || "Đã thanh toán".equals(r.getPaymentStatus())) {
            updateCustomerDebt(r, false, false, true);
        }
        
        receiptRepository.save(r);
        auditLogService.logAction(currentUser, "APPROVE", "receipts", String.valueOf(r.getId()), "Hoàn tất phiếu " + r.getType().name() + " " + r.getCode() + " -> COMPLETED");
        return new ReceiptResponse(r);
    }

    @Transactional
    @Override
    public ReceiptResponse confirmStocktake(Integer id, java.util.Map<String, Object> payload, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (r.getType() != ReceiptType.IMPORT && r.getType() != ReceiptType.TRANSFER) {
            throw new RuntimeException("Chỉ áp dụng cho phiếu Nhập kho hoặc Điều chuyển.");
        }
        if (r.getStatus() != ReceiptStatus.PENDING_STOCKTAKE) {
            throw new RuntimeException("Phiếu chưa ở trạng thái chờ kiểm kê.");
        }

        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");
            if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                throw new RuntimeException("Chỉ chi nhánh nhận mới được xác nhận kiểm kê.");
            }
        }

        java.util.Map<String, Integer> actualQuantities = new java.util.HashMap<>();
        java.util.Map<String, String> shortfallReasons = new java.util.HashMap<>();
        if (payload.containsKey("items")) {
            java.util.List<java.util.Map<String, Object>> items = (java.util.List<java.util.Map<String, Object>>) payload.get("items");
            for (java.util.Map<String, Object> item : items) {
                if (item.containsKey("receiptDetailId") && item.containsKey("actualQuantity")) {
                    String detailId = item.get("receiptDetailId").toString();
                    actualQuantities.put(detailId, Integer.valueOf(item.get("actualQuantity").toString()));
                    if (item.containsKey("shortfallReason") && item.get("shortfallReason") != null) {
                        shortfallReasons.put(detailId, item.get("shortfallReason").toString());
                    }
                }
            }
        }

        boolean hasShortfall = false;
        for (ReceiptDetail d : r.getDetails()) {
            String detailIdStr = d.getId().toString();
            Integer actualQty = actualQuantities.get(detailIdStr);
            if (actualQty == null) actualQty = d.getQuantity();
            if (actualQty > d.getQuantity()) {
                throw new RuntimeException("Số lượng nhận không được vượt quá số lượng trên phiếu.");
            }
            d.setReceivedQuantity(actualQty);
            if (actualQty < d.getQuantity()) {
                if (r.getType() != ReceiptType.IMPORT && r.getType() != ReceiptType.TRANSFER) {
                    throw new RuntimeException("Chức năng báo hao hụt hiện tại chỉ áp dụng cho phiếu Nhập kho và Điều chuyển.");
                }
                String reason = shortfallReasons.get(detailIdStr);
                if (reason == null || reason.trim().isEmpty()) {
                    throw new RuntimeException("Phải nhập lý do hao hụt cho sản phẩm bị thiếu: " + d.getProduct().getName());
                }
                d.setShortfallReason(reason);
                hasShortfall = true;
            }
            if (actualQty > 0) {
                addInventory(r.getDestBranch(), d, actualQty);
            }
            // Việc trừ kho ở chi nhánh nguồn đã được thực hiện lúc duyệt phiếu (chuyển sang PENDING_ADMIN/PENDING_STOCKTAKE)
        }

        r.setStocktakeBy(currentUser);
        if (hasShortfall) {
            r.setStatus(ReceiptStatus.PENDING_SHORTFALL_MANAGER);
            // Do not update debt yet, wait for final shortfall approval
            if (r.getType() == ReceiptType.TRANSFER) {
                r.setPaymentStatus("RECEIVED");
            }
        } else {
            updateCustomerDebt(r, true, false, false);
            r.setStatus(ReceiptStatus.COMPLETED);
            if (r.getType() == ReceiptType.TRANSFER) {
                r.setPaymentStatus("RECEIVED");
            }
        }

        receiptRepository.save(r);
        auditLogService.logAction(currentUser, "CONFIRM_STOCKTAKE", "receipts", String.valueOf(r.getId()),
                "Xác nhận kiểm kê nhận hàng cho phiếu " + r.getCode() + ". Kết quả: " + (hasShortfall ? "Có hao hụt" : "Đầy đủ/Khớp"));
        return new ReceiptResponse(r);
    }

    @Transactional
    @Override
    public ReceiptResponse approveShortfall(Integer id, boolean isApproved, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        
        if (r.getStatus() == ReceiptStatus.PENDING_SHORTFALL_MANAGER) {
            if (currentUser.getRole() != UserRole.MANAGER) {
                throw new RuntimeException("Chỉ Quản lý chi nhánh nhận mới có quyền duyệt thiếu hụt bước 1.");
            }
            if (currentUser.getRole() == UserRole.MANAGER) {
                Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
                if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Chỉ Quản lý chi nhánh nhận mới được duyệt thiếu hụt.");
                }
            }
            
            if (isApproved) {
                r.setStatus(ReceiptStatus.PENDING_SHORTFALL_ADMIN);
                receiptRepository.save(r);
            } else {
                // Manager rejects -> Cancel receipt and revert inventory added
                r.setStatus(ReceiptStatus.CANCELLED);
                for (ReceiptDetail d : r.getDetails()) {
                    if (d.getReceivedQuantity() != null && d.getReceivedQuantity() > 0) {
                        addInventory(r.getDestBranch(), d, -d.getReceivedQuantity());
                    }
                }
                // Source inventory was deducted, we need to revert it!
                boolean shouldRevertSource = true;
                if (r.getType() == ReceiptType.IMPORT) {
                    boolean isCrossBranch = (r.getSourceBranch() != null && r.getDestBranch() != null && !r.getSourceBranch().getId().equals(r.getDestBranch().getId()));
                    if (!isCrossBranch) {
                        shouldRevertSource = false;
                    }
                }
                if (shouldRevertSource) {
                    for (ReceiptDetail d : r.getDetails()) {
                        addInventory(r.getSourceBranch(), d, d.getQuantity());
                    }
                }
                receiptRepository.save(r);
            }
        } else if (r.getStatus() == ReceiptStatus.PENDING_SHORTFALL_ADMIN) {
            if (r.getType() == ReceiptType.TRANSFER) {
                if (currentUser.getRole() != UserRole.MANAGER) {
                    throw new RuntimeException("Chỉ Quản lý chi nhánh nguồn mới có quyền duyệt đền bù thiếu hụt.");
                }
                Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
                if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Chỉ Quản lý chi nhánh nguồn mới được duyệt đền bù thiếu hụt.");
                }
            } else {
                if (currentUser.getRole() != UserRole.ADMIN) {
                    throw new RuntimeException("Chỉ Admin mới có quyền duyệt thiếu hụt bước cuối.");
                }
            }
            if (!isApproved) {
                throw new RuntimeException("Admin không thể từ chối phiếu thiếu hụt (phiếu đã lên Admin thì không thể hủy được).");
            }
            
            // Admin approves -> Tự động sinh phiếu bù và hoàn tất phiếu gốc
            boolean isInternalTransfer = r.getSourceBranch() != null;
            if (isInternalTransfer) {
                // Tạo phiếu điều chuyển bù
                Receipt newTransfer = new Receipt();
                newTransfer.setCode("COMP-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                newTransfer.setType(ReceiptType.TRANSFER);
                newTransfer.setStatus(ReceiptStatus.PENDING_STOCKTAKE); // Đẩy thẳng lên chờ Kiểm kê (hàng đang trên đường đi)
                newTransfer.setSourceBranch(r.getSourceBranch());
                newTransfer.setDestBranch(r.getDestBranch());
                newTransfer.setCreatedBy(currentUser);
                newTransfer.setCreatedAt(java.time.LocalDateTime.now());
                newTransfer.setDescription("Phiếu điều chuyển bù hao hụt cho phiếu " + r.getCode());
                
                java.util.List<ReceiptDetail> newDetails = new java.util.ArrayList<>();
                for (ReceiptDetail d : r.getDetails()) {
                    if (d.getReceivedQuantity() != null && d.getReceivedQuantity() < d.getQuantity()) {
                        int shortfallQty = d.getQuantity() - d.getReceivedQuantity();
                        
                        // Trừ tồn kho tại chi nhánh nguồn
                        addInventory(r.getSourceBranch(), d, -shortfallQty);
                        
                        // Không cộng thẳng vào kho đích nữa, để cho staff kiểm kê phiếu bù
                        
                        ReceiptDetail newDetail = new ReceiptDetail();
                        newDetail.setReceipt(newTransfer);
                        newDetail.setProduct(d.getProduct());
                        newDetail.setQuantity(shortfallQty);
                        newDetail.setPrice(d.getPrice());
                        newDetail.setManufacturingDate(d.getManufacturingDate());
                        newDetail.setExpirationDate(d.getExpirationDate());
                        newDetails.add(newDetail);
                    }
                }
                
                if (newDetails.isEmpty()) {
                    throw new RuntimeException("Không tìm thấy hàng hóa nào bị thiếu hụt.");
                }
                
                newTransfer.setDetails(newDetails);
                receiptRepository.save(newTransfer);
            }
            r.setStatus(ReceiptStatus.COMPLETED);
            updateCustomerDebt(r, true, false, false);
            receiptRepository.save(r);
        } else {
            throw new RuntimeException("Phiếu không ở trạng thái chờ duyệt thiếu hụt.");
        }
        auditLogService.logAction(currentUser, isApproved ? "APPROVE_SHORTFALL" : "REJECT_SHORTFALL", "receipts", String.valueOf(r.getId()),
                (isApproved ? "Duyệt" : "Từ chối") + " hao hụt cho phiếu " + r.getCode() + ". Trạng thái mới: " + r.getStatus());
        return new ReceiptResponse(r);
    }

    @Transactional
    @Override
    public ReceiptResponse compensateShortfall(Integer id, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (r.getStatus() != ReceiptStatus.PENDING_COMPENSATION) {
            throw new RuntimeException("Phiếu không ở trạng thái chờ bù hao hụt.");
        }
        
        if (currentUser.getRole() != UserRole.MANAGER && currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Chỉ Quản lý chi nhánh tổng mới có quyền tạo phiếu điều chuyển bù.");
        }
        Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
        if (currentUser.getRole() == UserRole.MANAGER) {
            if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                throw new RuntimeException("Chỉ Quản lý chi nhánh tổng (nguồn) mới được thao tác.");
            }
        }

        // Tạo phiếu điều chuyển mới
        Receipt newTransfer = new Receipt();
        newTransfer.setCode("COMP-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        newTransfer.setType(ReceiptType.TRANSFER);
        newTransfer.setStatus(ReceiptStatus.PENDING_ADMIN); // Đẩy thẳng lên chờ Admin duyệt
        newTransfer.setSourceBranch(r.getSourceBranch());
        newTransfer.setDestBranch(r.getDestBranch());
        newTransfer.setCreatedBy(currentUser);
        newTransfer.setCreatedAt(java.time.LocalDateTime.now());
        newTransfer.setDescription("Phiếu điều chuyển bù hao hụt cho phiếu " + r.getCode());
        
        java.util.List<ReceiptDetail> newDetails = new java.util.ArrayList<>();
        for (ReceiptDetail d : r.getDetails()) {
            if (d.getReceivedQuantity() != null && d.getReceivedQuantity() < d.getQuantity()) {
                int shortfallQty = d.getQuantity() - d.getReceivedQuantity();
                
                // Trừ tồn kho tại chi nhánh nguồn
                addInventory(r.getSourceBranch(), d, -shortfallQty);
                
                ReceiptDetail newDetail = new ReceiptDetail();
                newDetail.setReceipt(newTransfer);
                newDetail.setProduct(d.getProduct());
                newDetail.setQuantity(shortfallQty);
                newDetail.setPrice(d.getPrice());
                newDetail.setManufacturingDate(d.getManufacturingDate());
                newDetail.setExpirationDate(d.getExpirationDate());
                newDetails.add(newDetail);
            }
        }
        
        if (newDetails.isEmpty()) {
            throw new RuntimeException("Không tìm thấy hàng hóa nào bị thiếu hụt.");
        }
        
        newTransfer.setDetails(newDetails);
        receiptRepository.save(newTransfer);
        
        // Cập nhật phiếu gốc thành COMPLETED
        r.setStatus(ReceiptStatus.COMPLETED);
        // Cập nhật công nợ nếu có
        updateCustomerDebt(r, true, false, false);
        receiptRepository.save(r);
        auditLogService.logAction(currentUser, "COMPENSATE_SHORTFALL", "receipts", String.valueOf(r.getId()),
                "Duyệt tạo phiếu bù điều chuyển hao hụt cho phiếu " + r.getCode() + ". Phiếu bù mới: " + newTransfer.getCode());
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
        auditLogService.logAction(currentUser, "MARK_PAID", "receipts", String.valueOf(r.getId()), "Xác nhận thanh toán phiếu " + r.getType().name() + " " + r.getCode() + " -> PAID");
        return new ReceiptResponse(r);
    }

    @Transactional
    @Override
    public ReceiptResponse confirmTransfer(Integer id, java.util.Map<String, Object> payload, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        
        Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
        if (myBranchId == null) {
            throw new RuntimeException("Bạn chưa thuộc chi nhánh nào. Không thể xác nhận nhận hàng.");
        }
        if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
            throw new RuntimeException("Chỉ nhân viên thuộc chi nhánh đích mới có quyền xác nhận nhận hàng.");
        }

        if ("RECEIVED".equals(r.getPaymentStatus())) {
            throw new RuntimeException("Phiếu này đã được xác nhận nhận hàng.");
        }

        java.util.Map<String, Integer> actualQuantities = new java.util.HashMap<>();
        java.util.Map<String, String> shortfallReasons = new java.util.HashMap<>();
        if (payload.containsKey("items")) {
            java.util.List<java.util.Map<String, Object>> items = (java.util.List<java.util.Map<String, Object>>) payload.get("items");
            for (java.util.Map<String, Object> item : items) {
                if (item.containsKey("receiptDetailId") && item.containsKey("actualQuantity")) {
                    String detailId = item.get("receiptDetailId").toString();
                    actualQuantities.put(detailId, Integer.valueOf(item.get("actualQuantity").toString()));
                    if (item.containsKey("shortfallReason") && item.get("shortfallReason") != null) {
                        shortfallReasons.put(detailId, item.get("shortfallReason").toString());
                    }
                }
            }
        }

        for (ReceiptDetail d : r.getDetails()) {
            String detailIdStr = d.getId().toString();
            Integer actualQty = actualQuantities.get(detailIdStr);
            if (actualQty == null) actualQty = d.getQuantity(); // Default to full if not provided
            if (actualQty > d.getQuantity()) {
                throw new RuntimeException("Số lượng nhận không được vượt quá số lượng gửi.");
            }
            d.setReceivedQuantity(actualQty);
            if (actualQty < d.getQuantity()) {
                String reason = shortfallReasons.get(detailIdStr);
                if (reason == null || reason.trim().isEmpty()) {
                    throw new RuntimeException("Phải nhập lý do hao hụt cho sản phẩm bị thiếu: " + d.getProduct().getName());
                }
                d.setShortfallReason(reason);
            }
            if (actualQty > 0) {
                addInventory(r.getDestBranch(), d, actualQty);
            }
        }

        r.setPaymentStatus("RECEIVED");
        receiptRepository.save(r);
        auditLogService.logAction(currentUser, "CONFIRM_TRANSFER", "receipts", String.valueOf(r.getId()),
                "Xác nhận nhận hàng điều chuyển cho phiếu " + r.getCode() + ". Trạng thái thanh toán/nhận: RECEIVED");
        return new ReceiptResponse(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getCompletedBranchReceipts(User currentUser) {
        boolean isGlobalUser = currentUser.getRole() == UserRole.ADMIN || 
                               (currentUser.getBranch() != null && currentUser.getBranch().getId() == 1);
        if (isGlobalUser) {
            return receiptRepository.findAll().stream()
                    .filter(r -> r.getStatus() == ReceiptStatus.COMPLETED)
                    .map(ReceiptResponse::new)
                    .collect(Collectors.toList());
        }

        Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
        if (myBranchId == null) throw new RuntimeException("Bạn chưa thuộc chi nhánh nào.");

        return receiptRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReceiptStatus.COMPLETED &&
                             ((r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) ||
                              (r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId))))
                .map(ReceiptResponse::new)
                .collect(Collectors.toList());
    }
}
