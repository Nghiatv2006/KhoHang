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
                        if (r.getStatus() == ReceiptStatus.DRAFT) {
                            boolean isMyBranch = myBranchId != null && 
                                ((r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId)) || 
                                 (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)));
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
        if (myBranchId == null) throw new RuntimeException("Bß║ín ch╞░a thuß╗Öc chi nh├ính n├áo.");

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
                                             && (r.getStatus() == ReceiptStatus.PENDING_STOCKTAKE || r.getStatus() == ReceiptStatus.COMPLETED || r.getStatus() == ReceiptStatus.PENDING_SHORTFALL_MANAGER || r.getStatus() == ReceiptStatus.PENDING_SHORTFALL_ADMIN);
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
            if (r.getStatus() == ReceiptStatus.DRAFT) {
                boolean isMyBranch = myBranchId != null && 
                    ((r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId)) || 
                     (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)));
                boolean isCreator = r.getCreatedBy() != null && r.getCreatedBy().getId().equals(currentUser.getId());
                if (!isMyBranch && !isCreator) {
                    throw new RuntimeException("Phiß║┐u nh├íp n├áy kh├┤ng thuß╗Öc quyß╗ün quß║ún l├╜ cß╗ºa bß║ín.");
                }
            }
            if (r.getType() == ReceiptType.TRANSFER) {
                if (myBranchId == null) throw new RuntimeException("Bß║ín kh├┤ng c├│ quyß╗ün xem phiß║┐u n├áy.");
                boolean hasPerm = (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) ||
                                  (r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId));
                if (!hasPerm) throw new RuntimeException("Bß║ín kh├┤ng c├│ quyß╗ün xem phiß║┐u n├áy.");
            }
        }
        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bß║ín ch╞░a thuß╗Öc chi nh├ính n├áo.");
            
            boolean hasPerm = (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) ||
                              (r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId));
            if (!hasPerm) throw new RuntimeException("Bß║ín kh├┤ng c├│ quyß╗ün xem phiß║┐u n├áy.");
            
            if (currentUser.getRole() == UserRole.STAFF) {
                if (r.getType() == ReceiptType.TRANSFER && r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Bß║ín kh├┤ng c├│ quyß╗ün xem phiß║┐u n├áy.");
                }
                boolean isMyReceipt = r.getCreatedBy() != null && r.getCreatedBy().getId().equals(currentUser.getId());
                boolean isIncoming = (r.getType() == ReceiptType.IMPORT || r.getType() == ReceiptType.TRANSFER) 
                                     && r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId)
                                     && (r.getStatus() == ReceiptStatus.PENDING_STOCKTAKE || r.getStatus() == ReceiptStatus.COMPLETED || r.getStatus() == ReceiptStatus.PENDING_SHORTFALL_MANAGER || r.getStatus() == ReceiptStatus.PENDING_SHORTFALL_ADMIN);
                if (!isMyReceipt && !isIncoming) {
                    throw new RuntimeException("Bß║ín kh├┤ng c├│ quyß╗ün xem phiß║┐u n├áy.");
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
                        if (r.getStatus() == ReceiptStatus.DRAFT) {
                            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
                            boolean isMyBranch = myBranchId != null && 
                                ((r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId)) || 
                                 (r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)));
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
                    throw new RuntimeException("T├¬n kh├ích h├áng l├á bß║»t buß╗Öc ─æß╗æi vß╗¢i phiß║┐u xuß║Ñt b├ín.");
                }
                if (request.getCustomerPhone() == null || request.getCustomerPhone().trim().isEmpty()) {
                    throw new RuntimeException("Sß╗æ ─æiß╗çn thoß║íi kh├ích h├áng l├á bß║»t buß╗Öc ─æß╗æi vß╗¢i phiß║┐u xuß║Ñt b├ín.");
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
                throw new RuntimeException("Kh├ích h├áng l├á bß║»t buß╗Öc ─æß╗æi vß╗¢i phiß║┐u xuß║Ñt b├ín.");
            }
        }
        r.setDescription(request.getDescription());

        // DISPOSAL-specific fields
        if (request.getType() == ReceiptType.DISPOSAL) {
            if (request.getDisposalReason() == null || request.getDisposalReason().trim().isEmpty()) {
                throw new RuntimeException("L├╜ do ti├¬u hß╗ºy l├á bß║»t buß╗Öc.");
            }
            r.setDisposalReason(request.getDisposalReason());
            r.setDisposalMethod(request.getDisposalMethod());
            r.setAttachmentUrl(request.getAttachmentUrl());
        }

        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bß║ín ch╞░a thuß╗Öc chi nh├ính n├áo.");

            if (currentUser.getRole() == UserRole.MANAGER) {
                throw new RuntimeException("Quß║ún l├╜ chß╗ë ─æ╞░ß╗úc ph├⌐p duyß╗çt phiß║┐u, viß╗çc lß║¡p phiß║┐u do Nh├ón vi├¬n thß╗▒c hiß╗çn.");
            }

            if (request.getType() == ReceiptType.IMPORT || request.getType() == ReceiptType.ADJUST_IN || request.getType() == ReceiptType.TRANSFER) {
                if (request.getDestBranchId() == null || !request.getDestBranchId().equals(myBranchId)) {
                    throw new RuntimeException("Bß║ín chß╗ë ─æ╞░ß╗úc ph├⌐p lß║¡p phiß║┐u cho chi nh├ính ─æ├¡ch l├á chi nh├ính cß╗ºa m├¼nh.");
                }
            } else {
                if (request.getSourceBranchId() == null || !request.getSourceBranchId().equals(myBranchId)) {
                    throw new RuntimeException("Bß║ín chß╗ë ─æ╞░ß╗úc ph├⌐p lß║¡p phiß║┐u xuß║Ñt/giß║úm tß╗½ kho cß╗ºa chi nh├ính m├¼nh.");
                }
            }
        }

        if ((request.getType() == ReceiptType.IMPORT || request.getType() == ReceiptType.ADJUST_IN) && request.getDestBranchId() == null) {
             throw new RuntimeException("Chi nh├ính ─æ├¡ch l├á bß║»t buß╗Öc.");
        }
        if ((request.getType() == ReceiptType.EXPORT || request.getType() == ReceiptType.TRANSFER || request.getType() == ReceiptType.ADJUST_OUT || request.getType() == ReceiptType.DISPOSAL) && request.getSourceBranchId() == null) {
             throw new RuntimeException("Kho xuß║Ñt/nguß╗ôn l├á bß║»t buß╗Öc.");
        }
        if (request.getType() == ReceiptType.TRANSFER && request.getDestBranchId() == null) {
             throw new RuntimeException("Chi nh├ính nhß║¡n l├á bß║»t buß╗Öc ─æß╗æi vß╗¢i phiß║┐u ─æiß╗üu chuyß╗ân.");
        }
        if (request.getType() == ReceiptType.TRANSFER && request.getSourceBranchId().equals(request.getDestBranchId())) {
             throw new RuntimeException("Kho xuß║Ñt v├á nhß║¡n phß║úi kh├íc nhau.");
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
                    throw new RuntimeException("Sß║ún phß║⌐m " + pName + " chß╗ë c├▓n " + totalQty + " trong kho nguß╗ôn, kh├┤ng ─æß╗º ─æß╗â lß║¡p phiß║┐u.");
                }
            }
        }

        for (ReceiptDetailSaveRequest dReq : request.getDetails()) {
            ReceiptDetail d = new ReceiptDetail();
            d.setReceipt(r);
            Product p = productRepository.findById(dReq.getProductId()).orElseThrow(() -> new RuntimeException("Product not found"));
            
            if (p.getHasExpiry() && dReq.getManufacturingDate() != null && dReq.getExpirationDate() != null) {
                if (dReq.getManufacturingDate().isAfter(dReq.getExpirationDate())) {
                    throw new RuntimeException("Hß║ín sß╗¡ dß╗Ñng kh├┤ng hß╗úp lß╗ç (Ng├áy sß║ún xuß║Ñt kh├┤ng ─æ╞░ß╗úc lß╗¢n h╞ín Hß║ín sß╗¡ dß╗Ñng).");
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

        // Ghi Nhß║¡t k├╜
        String typeLabel = r.getType() != null ? r.getType().name() : "PHß»¬U";
        auditLogService.logAction(currentUser, "CREATE", "receipts",
                String.valueOf(r.getId()),
                "Tß║ío phiß║┐u " + typeLabel + ": " + r.getCode());

        return new ReceiptResponse(r);
    }

    @Transactional
    @Override
    public ReceiptResponse cancelReceipt(Integer id, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (r.getStatus() == ReceiptStatus.CANCELLED) throw new RuntimeException("Receipt is already cancelled.");
        if (r.getStatus() == ReceiptStatus.PENDING_SHORTFALL_ADMIN) throw new RuntimeException("Phiß║┐u ─æ├ú chuyß╗ân l├¬n Admin duyß╗çt thiß║┐u hß╗Ñt th├¼ kh├┤ng thß╗â hß╗ºy.");
        
        if (r.getStatus() == ReceiptStatus.DRAFT) {
            if (!currentUser.getId().equals(r.getCreatedBy().getId())) {
                throw new RuntimeException("Chß╗ë ng╞░ß╗¥i lß║¡p phiß║┐u mß╗¢i ─æ╞░ß╗úc quyß╗ün x├│a/hß╗ºy phiß║┐u nh├íp.");
            }
        } else {
            if (currentUser.getRole() == UserRole.STAFF) {
                throw new RuntimeException("Nh├ón vi├¬n kh├┤ng c├│ quyß╗ün hß╗ºy phiß║┐u ─æ├ú ─æ╞░ß╗úc xß╗¡ l├╜.");
            }
            
            if (currentUser.getRole() == UserRole.ADMIN) {
                if (r.getType() == ReceiptType.EXPORT && r.getSourceBranch() != null && r.getSourceBranch().getId() != 1) {
                    throw new RuntimeException("Admin chß╗ë quß║ún l├╜ phiß║┐u xuß║Ñt b├ín cß╗ºa Chi nh├ính tß╗òng.");
                }
            } else {
                Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
                if (myBranchId == null) throw new RuntimeException("Bß║ín ch╞░a thuß╗Öc chi nh├ính n├áo.");
                if (r.getType() == ReceiptType.IMPORT || r.getType() == ReceiptType.ADJUST_IN) {
                    boolean isCrossBranch = (r.getType() == ReceiptType.IMPORT && r.getSourceBranch() != null && r.getDestBranch() != null && !r.getSourceBranch().getId().equals(r.getDestBranch().getId()));
                    if (isCrossBranch) {
                        if (!r.getDestBranch().getId().equals(myBranchId) && !r.getSourceBranch().getId().equals(myBranchId)) {
                            throw new RuntimeException("Bß║ín kh├┤ng c├│ quyß╗ün hß╗ºy phiß║┐u cß╗ºa chi nh├ính kh├íc.");
                        }
                    } else {
                        if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                            throw new RuntimeException("Bß║ín kh├┤ng c├│ quyß╗ün hß╗ºy phiß║┐u cß╗ºa chi nh├ính kh├íc.");
                        }
                    }
                } else if (r.getType() == ReceiptType.TRANSFER) {
                    if ((r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) && 
                        (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId))) {
                        throw new RuntimeException("Bß║ín kh├┤ng c├│ quyß╗ün hß╗ºy phiß║┐u ─æiß╗üu chuyß╗ân cß╗ºa chi nh├ính kh├íc.");
                    }
                } else {
                    if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                        throw new RuntimeException("Bß║ín kh├┤ng c├│ quyß╗ün hß╗ºy phiß║┐u cß╗ºa chi nh├ính kh├íc.");
                    }
                }
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

        // Ghi Nhß║¡t k├╜
        auditLogService.logAction(currentUser, "CANCEL", "receipts",
                String.valueOf(r.getId()),
                "Hß╗ºy phiß║┐u: " + r.getCode());

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
                throw new RuntimeException("Kh├┤ng ─æß╗º tß╗ôn kho ─æß╗â thß╗▒c hiß╗çn giao dß╗ïch cho sß║ún phß║⌐m: " + detail.getProduct().getName());
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
                throw new RuntimeException("Kh├┤ng ─æß╗º tß╗ôn kho ─æß╗â thß╗▒c hiß╗çn giao dß╗ïch cho sß║ún phß║⌐m: " + detail.getProduct().getName());
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
            throw new RuntimeException("Phiß║┐u kh├┤ng ß╗ƒ trß║íng th├íi chß╗¥ duyß╗çt.");
        }
        if ((r.getType() == ReceiptType.IMPORT || r.getType() == ReceiptType.TRANSFER) && r.getStatus() != ReceiptStatus.DRAFT && r.getStatus() != ReceiptStatus.PENDING_ADMIN) {
            throw new RuntimeException("Phiß║┐u kh├┤ng ß╗ƒ trß║íng th├íi c├│ thß╗â duyß╗çt.");
        }
        if (r.getType() == ReceiptType.DISPOSAL && r.getStatus() != ReceiptStatus.DRAFT && r.getStatus() != ReceiptStatus.PENDING_ADMIN && r.getStatus() != ReceiptStatus.PENDING_STOCKTAKE) {
            throw new RuntimeException("Phiß║┐u ti├¬u hß╗ºy kh├┤ng ß╗ƒ trß║íng th├íi c├│ thß╗â duyß╗çt.");
        }
        
        if (currentUser.getRole() == UserRole.ADMIN && r.getType() == ReceiptType.TRANSFER) {
            throw new RuntimeException("Admin kh├┤ng ─æ╞░ß╗úc quyß╗ün can thiß╗çp v├áo phiß║┐u ─æiß╗üu chuyß╗ân giß╗»a c├íc chi nh├ính.");
        }

        if (r.getType() == ReceiptType.EXPORT && r.getStatus() == ReceiptStatus.DRAFT) {
            if (!currentUser.getId().equals(r.getCreatedBy().getId())) {
                throw new RuntimeException("Chß╗ë ng╞░ß╗¥i lß║¡p H├│a ─æ╞ín mß╗¢i ─æ╞░ß╗úc quyß╗ün ho├án th├ánh (duyß╗çt) H├│a ─æ╞ín nh├íp.");
            }
        }

        if (currentUser.getRole() == UserRole.STAFF) {
            if (r.getType() == ReceiptType.DISPOSAL) {
                if (r.getStatus() != ReceiptStatus.DRAFT) {
                    throw new RuntimeException("Phiß║┐u ti├¬u hß╗ºy bß║»t buß╗Öc phß║úi do Quß║ún l├╜ duyß╗çt.");
                }
            } else if (r.getType() != ReceiptType.ADJUST_OUT && r.getType() != ReceiptType.EXPORT) {
                throw new RuntimeException("Nh├ón vi├¬n kh├┤ng c├│ quyß╗ün duyß╗çt phiß║┐u n├áy.");
            }
        }
        
        if (currentUser.getRole() == UserRole.ADMIN) {
            if (r.getType() == ReceiptType.EXPORT && r.getSourceBranch() != null && r.getSourceBranch().getId() != 1) {
                throw new RuntimeException("Admin chß╗ë quß║ún l├╜ phiß║┐u xuß║Ñt b├ín cß╗ºa Chi nh├ính tß╗òng.");
            }
        }

        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bß║ín ch╞░a thuß╗Öc chi nh├ính n├áo.");

            if (r.getType() == ReceiptType.IMPORT) {
                boolean isCrossBranch = (r.getSourceBranch() != null && r.getDestBranch() != null && !r.getSourceBranch().getId().equals(r.getDestBranch().getId()));
                if (isCrossBranch) {
                    if (!r.getSourceBranch().getId().equals(myBranchId) && !r.getDestBranch().getId().equals(myBranchId)) {
                        throw new RuntimeException("Chß╗ë Quß║ún l├╜ chi nh├ính li├¬n quan mß╗¢i c├│ quyß╗ün duyß╗çt phiß║┐u nhß║¡p kho.");
                    }
                } else {
                    if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                        throw new RuntimeException("Bß║ín kh├┤ng c├│ quyß╗ün thao t├íc tr├¬n phiß║┐u cß╗ºa chi nh├ính kh├íc.");
                    }
                }
            } else if (r.getType() == ReceiptType.TRANSFER) {
                if (r.getStatus() == ReceiptStatus.DRAFT) {
                    if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                        throw new RuntimeException("Chß╗ë Quß║ún l├╜ chi nh├ính ─æ├¡ch mß╗¢i c├│ quyß╗ün duyß╗çt y├¬u cß║ºu xin h├áng.");
                    }
                } else if (r.getStatus() == ReceiptStatus.PENDING_ADMIN) {
                    if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                        throw new RuntimeException("Chß╗ë Quß║ún l├╜ chi nh├ính nguß╗ôn mß╗¢i c├│ quyß╗ün duyß╗çt xuß║Ñt kho.");
                    }
                }
            } else if (r.getType() == ReceiptType.ADJUST_IN) {
                if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Bß║ín kh├┤ng c├│ quyß╗ün duyß╗çt phiß║┐u cß╗ºa chi nh├ính kh├íc.");
                }
            } else {
                // EXPORT, ADJUST_OUT, DISPOSAL: check source branch
                if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Bß║ín kh├┤ng c├│ quyß╗ün duyß╗çt phiß║┐u cß╗ºa chi nh├ính kh├íc.");
                }
            }
        }

        // ΓöÇΓöÇ DISPOSAL: Qu├í tr├¼nh duyß╗çt 2 b╞░ß╗¢c (Manager -> Admin) ΓöÇΓöÇ
        if (r.getType() == ReceiptType.DISPOSAL) {
            if (r.getStatus() == ReceiptStatus.DRAFT) {
                if (currentUser.getRole() == UserRole.MANAGER) {
                    r.setStatus(ReceiptStatus.PENDING_STOCKTAKE);
                    receiptRepository.save(r);
                    auditLogService.logAction(currentUser, "APPROVE", "receipts",
                            String.valueOf(r.getId()),
                            "Phiß║┐u ti├¬u hß╗ºy " + r.getCode() + " ─æ├ú ─æ╞░ß╗úc Quß║ún l├╜ duyß╗çt, chß╗¥ Admin x├íc nhß║¡n.");
                    return new ReceiptResponse(r);
                } else if (currentUser.getRole() != UserRole.ADMIN) {
                    r.setStatus(ReceiptStatus.PENDING_ADMIN);
                    receiptRepository.save(r);
                    return new ReceiptResponse(r);
                }
            } else if (r.getStatus() == ReceiptStatus.PENDING_ADMIN) {
                if (currentUser.getRole() != UserRole.MANAGER && currentUser.getRole() != UserRole.ADMIN) {
                    throw new RuntimeException("Chß╗ë Quß║ún l├╜ mß╗¢i c├│ quyß╗ün duyß╗çt phiß║┐u ti├¬u hß╗ºy.");
                }
                
                if (currentUser.getRole() == UserRole.MANAGER) {
                    r.setStatus(ReceiptStatus.PENDING_STOCKTAKE);
                    receiptRepository.save(r);
                    auditLogService.logAction(currentUser, "APPROVE", "receipts",
                            String.valueOf(r.getId()),
                            "Phiß║┐u ti├¬u hß╗ºy " + r.getCode() + " ─æ├ú ─æ╞░ß╗úc Quß║ún l├╜ duyß╗çt, chß╗¥ Admin x├íc nhß║¡n.");
                    return new ReceiptResponse(r);
                }
            } else if (r.getStatus() == ReceiptStatus.PENDING_STOCKTAKE) {
                if (currentUser.getRole() != UserRole.ADMIN) {
                    throw new RuntimeException("Chß╗ë Admin mß╗¢i c├│ quyß╗ün duyß╗çt cuß╗æi c├╣ng phiß║┐u ti├¬u hß╗ºy.");
                }
            }
        }

        if (r.getApprovedBy() == null || currentUser.getRole() != UserRole.ADMIN) {
            r.setApprovedBy(currentUser);
        }

        if (r.getType() == ReceiptType.IMPORT || r.getType() == ReceiptType.TRANSFER || r.getType() == ReceiptType.ADJUST_OUT) {
            if (r.getStatus() == ReceiptStatus.DRAFT) {
                // Mß╗¢i DRAFT -> PENDING_ADMIN (Hoß║╖c COMPLETED lu├┤n vß╗¢i ADJUST_OUT)
                if (r.getType() == ReceiptType.ADJUST_OUT) {
                    if (currentUser.getRole() == UserRole.STAFF) {
                        java.math.BigDecimal totalValue = java.math.BigDecimal.ZERO;
                        boolean hasMilk = false;
                        for (ReceiptDetail d : r.getDetails()) {
                            totalValue = totalValue.add(d.getPrice().multiply(java.math.BigDecimal.valueOf(d.getQuantity())));
                            if (d.getProduct() != null && d.getProduct().getCategory() != null) {
                                if (d.getProduct().getCategory().getName().toLowerCase().contains("sß╗»a")) {
                                    hasMilk = true;
                                }
                            }
                        }
                        if (!hasMilk && totalValue.compareTo(new java.math.BigDecimal("45000000")) >= 0) {
                            r.setStatus(ReceiptStatus.PENDING_ADMIN);
                            receiptRepository.save(r);
                            return new ReceiptResponse(r);
                        }
                    }
                    // Nß║┐u l├á Manager/Admin hoß║╖c Staff (d╞░ß╗¢i 45tr/Sß╗»a), phiß║┐u ADJUST_OUT sß║╜ bß╗Å qua PENDING_ADMIN v├á chß║íy xuß╗æng ─æß╗â COMPLETED
                } else {
                    if (currentUser.getRole() != UserRole.MANAGER && currentUser.getRole() != UserRole.ADMIN && currentUser.getRole() != UserRole.STAFF) {
                        throw new RuntimeException("Bß║ín kh├┤ng c├│ quyß╗ün duyß╗çt phiß║┐u ß╗ƒ b╞░ß╗¢c n├áy.");
                    }
                    if (r.getType() == ReceiptType.EXPORT) {
                        // Bß╗Å qua PENDING_ADMIN ─æß╗â chß║íy xuß╗æng cuß╗æi h├ám v├á set COMPLETED
                    } else {
                        if (r.getType() == ReceiptType.TRANSFER) {
                            // Kh├┤ng trß╗½ tß╗ôn kho ß╗ƒ b╞░ß╗¢c xin h├áng, chß╗¥ chi nh├ính Nguß╗ôn duyß╗çt mß╗¢i trß╗½
                        }
                        r.setStatus(ReceiptStatus.PENDING_ADMIN);
                        receiptRepository.save(r);
                        return new ReceiptResponse(r);
                    }
                }
            } else if (r.getStatus() == ReceiptStatus.PENDING_ADMIN) {
                if (r.getType() == ReceiptType.ADJUST_OUT) {
                    if (currentUser.getRole() == UserRole.STAFF) {
                        throw new RuntimeException("Chß╗ë Quß║ún l├╜ mß╗¢i c├│ quyß╗ün duyß╗çt phiß║┐u giß║úm tß╗ôn kho lß╗¢n.");
                    }
                    // Manager/Admin duyß╗çt -> Chß║íy xuß╗æng d├▓ng ─æß╗â COMPLETED
                } else {
                    // PENDING_ADMIN -> PENDING_STOCKTAKE cho IMPORT / TRANSFER
                    if (r.getType() == ReceiptType.IMPORT) {
                        if (currentUser.getRole() != UserRole.ADMIN) {
                            throw new RuntimeException("Chß╗ë Admin mß╗¢i c├│ quyß╗ün duyß╗çt l├¬n b╞░ß╗¢c Kiß╗âm k├¬.");
                        }

                        // Khi Admin duyß╗çt, h├áng h├│a ß╗ƒ chi nh├ính nguß╗ôn (kho tß╗òng) ch├¡nh thß╗⌐c bß╗ï trß╗½ ─æi
                        boolean isCrossBranch = (r.getSourceBranch() != null && r.getDestBranch() != null && !r.getSourceBranch().getId().equals(r.getDestBranch().getId()));
                        if (isCrossBranch) {
                            for (ReceiptDetail d : r.getDetails()) {
                                addInventory(r.getSourceBranch(), d, -d.getQuantity());
                            }
                        }
                    } else if (r.getType() == ReceiptType.TRANSFER) {
                        // Manager nguß╗ôn duyß╗çt xuß║Ñt kho -> trß╗½ tß╗ôn kho
                        for (ReceiptDetail d : r.getDetails()) {
                            addInventory(r.getSourceBranch(), d, -d.getQuantity());
                        }
                    }

                    r.setStatus(ReceiptStatus.PENDING_STOCKTAKE);
                    receiptRepository.save(r);
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
        if ("PAID".equals(r.getPaymentStatus()) || "─É├ú thanh to├ín".equals(r.getPaymentStatus())) {
            updateCustomerDebt(r, false, false, true);
        }
        
        receiptRepository.save(r);
        return new ReceiptResponse(r);
    }

    @Transactional
    @Override
    public ReceiptResponse confirmStocktake(Integer id, java.util.Map<String, Object> payload, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (r.getType() != ReceiptType.IMPORT && r.getType() != ReceiptType.TRANSFER) {
            throw new RuntimeException("Chß╗ë ├íp dß╗Ñng cho phiß║┐u Nhß║¡p kho hoß║╖c ─Éiß╗üu chuyß╗ân.");
        }
        if (r.getStatus() != ReceiptStatus.PENDING_STOCKTAKE) {
            throw new RuntimeException("Phiß║┐u ch╞░a ß╗ƒ trß║íng th├íi chß╗¥ kiß╗âm k├¬.");
        }

        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bß║ín ch╞░a thuß╗Öc chi nh├ính n├áo.");
            if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                throw new RuntimeException("Chß╗ë chi nh├ính nhß║¡n mß╗¢i ─æ╞░ß╗úc x├íc nhß║¡n kiß╗âm k├¬.");
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
                throw new RuntimeException("Sß╗æ l╞░ß╗úng nhß║¡n kh├┤ng ─æ╞░ß╗úc v╞░ß╗út qu├í sß╗æ l╞░ß╗úng tr├¬n phiß║┐u.");
            }
            d.setReceivedQuantity(actualQty);
            if (actualQty < d.getQuantity()) {
                if (r.getType() != ReceiptType.IMPORT && r.getType() != ReceiptType.TRANSFER) {
                    throw new RuntimeException("Chß╗⌐c n─âng b├ío hao hß╗Ñt hiß╗çn tß║íi chß╗ë ├íp dß╗Ñng cho phiß║┐u Nhß║¡p kho v├á ─Éiß╗üu chuyß╗ân.");
                }
                String reason = shortfallReasons.get(detailIdStr);
                if (reason == null || reason.trim().isEmpty()) {
                    throw new RuntimeException("Phß║úi nhß║¡p l├╜ do hao hß╗Ñt cho sß║ún phß║⌐m bß╗ï thiß║┐u: " + d.getProduct().getName());
                }
                d.setShortfallReason(reason);
                hasShortfall = true;
            }
            if (actualQty > 0) {
                addInventory(r.getDestBranch(), d, actualQty);
            }
            // Viß╗çc trß╗½ kho ß╗ƒ chi nh├ính nguß╗ôn ─æ├ú ─æ╞░ß╗úc thß╗▒c hiß╗çn l├║c duyß╗çt phiß║┐u (chuyß╗ân sang PENDING_ADMIN/PENDING_STOCKTAKE)
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
                "X├íc nhß║¡n kiß╗âm k├¬ nhß║¡n h├áng cho phiß║┐u " + r.getCode() + ". Kß║┐t quß║ú: " + (hasShortfall ? "C├│ hao hß╗Ñt" : "─Éß║ºy ─æß╗º/Khß╗¢p"));
        return new ReceiptResponse(r);
    }

    @Transactional
    @Override
    public ReceiptResponse approveShortfall(Integer id, boolean isApproved, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        
        if (r.getStatus() == ReceiptStatus.PENDING_SHORTFALL_MANAGER) {
            if (currentUser.getRole() != UserRole.MANAGER) {
                throw new RuntimeException("Chß╗ë Quß║ún l├╜ chi nh├ính nhß║¡n mß╗¢i c├│ quyß╗ün duyß╗çt thiß║┐u hß╗Ñt b╞░ß╗¢c 1.");
            }
            if (currentUser.getRole() == UserRole.MANAGER) {
                Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
                if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Chß╗ë Quß║ún l├╜ chi nh├ính nhß║¡n mß╗¢i ─æ╞░ß╗úc duyß╗çt thiß║┐u hß╗Ñt.");
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
                    throw new RuntimeException("Chß╗ë Quß║ún l├╜ chi nh├ính nguß╗ôn mß╗¢i c├│ quyß╗ün duyß╗çt ─æß╗ün b├╣ thiß║┐u hß╗Ñt.");
                }
                Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
                if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Chß╗ë Quß║ún l├╜ chi nh├ính nguß╗ôn mß╗¢i ─æ╞░ß╗úc duyß╗çt ─æß╗ün b├╣ thiß║┐u hß╗Ñt.");
                }
            } else {
                if (currentUser.getRole() != UserRole.ADMIN) {
                    throw new RuntimeException("Chß╗ë Admin mß╗¢i c├│ quyß╗ün duyß╗çt thiß║┐u hß╗Ñt b╞░ß╗¢c cuß╗æi.");
                }
            }
            if (!isApproved) {
                throw new RuntimeException("Admin kh├┤ng thß╗â tß╗½ chß╗æi phiß║┐u thiß║┐u hß╗Ñt (phiß║┐u ─æ├ú l├¬n Admin th├¼ kh├┤ng thß╗â hß╗ºy ─æ╞░ß╗úc).");
            }
            
            // Admin approves -> Tß╗▒ ─æß╗Öng sinh phiß║┐u b├╣ v├á ho├án tß║Ñt phiß║┐u gß╗æc
            boolean isInternalTransfer = r.getSourceBranch() != null;
            if (isInternalTransfer) {
                // Tß║ío phiß║┐u ─æiß╗üu chuyß╗ân b├╣
                Receipt newTransfer = new Receipt();
                newTransfer.setCode("COMP-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                newTransfer.setType(ReceiptType.TRANSFER);
                newTransfer.setStatus(ReceiptStatus.PENDING_STOCKTAKE); // ─Éß║⌐y thß║│ng l├¬n chß╗¥ Kiß╗âm k├¬ (h├áng ─æang tr├¬n ─æ╞░ß╗¥ng ─æi)
                newTransfer.setSourceBranch(r.getSourceBranch());
                newTransfer.setDestBranch(r.getDestBranch());
                newTransfer.setCreatedBy(currentUser);
                newTransfer.setCreatedAt(java.time.LocalDateTime.now());
                newTransfer.setDescription("Phiß║┐u ─æiß╗üu chuyß╗ân b├╣ hao hß╗Ñt cho phiß║┐u " + r.getCode());
                
                java.util.List<ReceiptDetail> newDetails = new java.util.ArrayList<>();
                for (ReceiptDetail d : r.getDetails()) {
                    if (d.getReceivedQuantity() != null && d.getReceivedQuantity() < d.getQuantity()) {
                        int shortfallQty = d.getQuantity() - d.getReceivedQuantity();
                        
                        // Trß╗½ tß╗ôn kho tß║íi chi nh├ính nguß╗ôn
                        addInventory(r.getSourceBranch(), d, -shortfallQty);
                        
                        // Kh├┤ng cß╗Öng thß║│ng v├áo kho ─æ├¡ch nß╗»a, ─æß╗â cho staff kiß╗âm k├¬ phiß║┐u b├╣
                        
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
                    throw new RuntimeException("Kh├┤ng t├¼m thß║Ñy h├áng h├│a n├áo bß╗ï thiß║┐u hß╗Ñt.");
                }
                
                newTransfer.setDetails(newDetails);
                receiptRepository.save(newTransfer);
            }
            r.setStatus(ReceiptStatus.COMPLETED);
            updateCustomerDebt(r, true, false, false);
            receiptRepository.save(r);
        } else {
            throw new RuntimeException("Phiß║┐u kh├┤ng ß╗ƒ trß║íng th├íi chß╗¥ duyß╗çt thiß║┐u hß╗Ñt.");
        }
        auditLogService.logAction(currentUser, isApproved ? "APPROVE_SHORTFALL" : "REJECT_SHORTFALL", "receipts", String.valueOf(r.getId()),
                (isApproved ? "Duyß╗çt" : "Tß╗½ chß╗æi") + " hao hß╗Ñt cho phiß║┐u " + r.getCode() + ". Trß║íng th├íi mß╗¢i: " + r.getStatus());
        return new ReceiptResponse(r);
    }

    @Transactional
    @Override
    public ReceiptResponse compensateShortfall(Integer id, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (r.getStatus() != ReceiptStatus.PENDING_COMPENSATION) {
            throw new RuntimeException("Phiß║┐u kh├┤ng ß╗ƒ trß║íng th├íi chß╗¥ b├╣ hao hß╗Ñt.");
        }
        
        if (currentUser.getRole() != UserRole.MANAGER && currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Chß╗ë Quß║ún l├╜ chi nh├ính tß╗òng mß╗¢i c├│ quyß╗ün tß║ío phiß║┐u ─æiß╗üu chuyß╗ân b├╣.");
        }
        Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
        if (currentUser.getRole() == UserRole.MANAGER) {
            if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                throw new RuntimeException("Chß╗ë Quß║ún l├╜ chi nh├ính tß╗òng (nguß╗ôn) mß╗¢i ─æ╞░ß╗úc thao t├íc.");
            }
        }

        // Tß║ío phiß║┐u ─æiß╗üu chuyß╗ân mß╗¢i
        Receipt newTransfer = new Receipt();
        newTransfer.setCode("COMP-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        newTransfer.setType(ReceiptType.TRANSFER);
        newTransfer.setStatus(ReceiptStatus.PENDING_ADMIN); // ─Éß║⌐y thß║│ng l├¬n chß╗¥ Admin duyß╗çt
        newTransfer.setSourceBranch(r.getSourceBranch());
        newTransfer.setDestBranch(r.getDestBranch());
        newTransfer.setCreatedBy(currentUser);
        newTransfer.setCreatedAt(java.time.LocalDateTime.now());
        newTransfer.setDescription("Phiß║┐u ─æiß╗üu chuyß╗ân b├╣ hao hß╗Ñt cho phiß║┐u " + r.getCode());
        
        java.util.List<ReceiptDetail> newDetails = new java.util.ArrayList<>();
        for (ReceiptDetail d : r.getDetails()) {
            if (d.getReceivedQuantity() != null && d.getReceivedQuantity() < d.getQuantity()) {
                int shortfallQty = d.getQuantity() - d.getReceivedQuantity();
                
                // Trß╗½ tß╗ôn kho tß║íi chi nh├ính nguß╗ôn
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
            throw new RuntimeException("Kh├┤ng t├¼m thß║Ñy h├áng h├│a n├áo bß╗ï thiß║┐u hß╗Ñt.");
        }
        
        newTransfer.setDetails(newDetails);
        receiptRepository.save(newTransfer);
        
        // Cß║¡p nhß║¡t phiß║┐u gß╗æc th├ánh COMPLETED
        r.setStatus(ReceiptStatus.COMPLETED);
        // Cß║¡p nhß║¡t c├┤ng nß╗ú nß║┐u c├│
        updateCustomerDebt(r, true, false, false);
        receiptRepository.save(r);
        auditLogService.logAction(currentUser, "COMPENSATE_SHORTFALL", "receipts", String.valueOf(r.getId()),
                "Duyß╗çt tß║ío phiß║┐u b├╣ ─æiß╗üu chuyß╗ân hao hß╗Ñt cho phiß║┐u " + r.getCode() + ". Phiß║┐u b├╣ mß╗¢i: " + newTransfer.getCode());
        return new ReceiptResponse(r);
    }

    @Transactional
    @Override
    public ReceiptResponse markPaid(Integer id, User currentUser) {
        Receipt r = receiptRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        
        if (currentUser.getRole() != UserRole.ADMIN) {
            Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
            if (myBranchId == null) throw new RuntimeException("Bß║ín ch╞░a thuß╗Öc chi nh├ính n├áo.");
            
            if (r.getType() == ReceiptType.EXPORT) {
                if (r.getSourceBranch() == null || !r.getSourceBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Chß╗ë chi nh├ính xuß║Ñt mß╗¢i ─æ╞░ß╗úc x├íc nhß║¡n thanh to├ín.");
                }
            } else if (r.getType() == ReceiptType.IMPORT) {
                if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
                    throw new RuntimeException("Chß╗ë chi nh├ính nhß║¡p mß╗¢i ─æ╞░ß╗úc x├íc nhß║¡n thanh to├ín.");
                }
            }
        }

        if (!"PAID".equals(r.getPaymentStatus()) && !"─É├ú thanh to├ín".equals(r.getPaymentStatus())) {
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
        
        Integer myBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : null;
        if (myBranchId == null) {
            throw new RuntimeException("Bß║ín ch╞░a thuß╗Öc chi nh├ính n├áo. Kh├┤ng thß╗â x├íc nhß║¡n nhß║¡n h├áng.");
        }
        if (r.getDestBranch() == null || !r.getDestBranch().getId().equals(myBranchId)) {
            throw new RuntimeException("Chß╗ë nh├ón vi├¬n thuß╗Öc chi nh├ính ─æ├¡ch mß╗¢i c├│ quyß╗ün x├íc nhß║¡n nhß║¡n h├áng.");
        }

        if ("RECEIVED".equals(r.getPaymentStatus())) {
            throw new RuntimeException("Phiß║┐u n├áy ─æ├ú ─æ╞░ß╗úc x├íc nhß║¡n nhß║¡n h├áng.");
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
                throw new RuntimeException("Sß╗æ l╞░ß╗úng nhß║¡n kh├┤ng ─æ╞░ß╗úc v╞░ß╗út qu├í sß╗æ l╞░ß╗úng gß╗¡i.");
            }
            d.setReceivedQuantity(actualQty);
            if (actualQty < d.getQuantity()) {
                String reason = shortfallReasons.get(detailIdStr);
                if (reason == null || reason.trim().isEmpty()) {
                    throw new RuntimeException("Phß║úi nhß║¡p l├╜ do hao hß╗Ñt cho sß║ún phß║⌐m bß╗ï thiß║┐u: " + d.getProduct().getName());
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
                "X├íc nhß║¡n nhß║¡n h├áng ─æiß╗üu chuyß╗ân cho phiß║┐u " + r.getCode() + ". Trß║íng th├íi thanh to├ín/nhß║¡n: RECEIVED");
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
        if (myBranchId == null) throw new RuntimeException("Bß║ín ch╞░a thuß╗Öc chi nh├ính n├áo.");

        return receiptRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReceiptStatus.COMPLETED &&
                             ((r.getSourceBranch() != null && r.getSourceBranch().getId().equals(myBranchId)) ||
                              (r.getDestBranch() != null && r.getDestBranch().getId().equals(myBranchId))))
                .map(ReceiptResponse::new)
                .collect(Collectors.toList());
    }
}
