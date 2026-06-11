package com.example.Hehe.service;

import com.example.Hehe.dto.SupplierResponse;
import com.example.Hehe.dto.SupplierSaveRequest;
import com.example.Hehe.model.Supplier;
import com.example.Hehe.model.User;
import com.example.Hehe.repository.SupplierRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public SupplierServiceImpl(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> searchSuppliers(String keyword, String status) {
        String pattern = (keyword != null && !keyword.trim().isEmpty()) ? "%" + keyword.trim().toLowerCase() + "%" : null;
        String filterStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : null;

        return supplierRepository.searchSuppliers(pattern, filterStatus)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Integer id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà cung cấp với ID: " + id));
        return convertToResponse(supplier);
    }

    @Override
    public SupplierResponse createSupplier(SupplierSaveRequest request, User currentUser) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên nhà cung cấp không được để trống.");
        }

        Supplier supplier = new Supplier();
        supplier.setName(request.getName().trim());
        supplier.setContactInfo(request.getContactInfo() != null ? request.getContactInfo().trim() : null);
        supplier.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        supplier.setDebt(request.getDebt() != null ? request.getDebt() : BigDecimal.ZERO);
        supplier.setStatus("ACTIVE");

        Supplier savedSupplier = supplierRepository.save(supplier);

        // Ghi audit log
        logAudit(currentUser.getId(), "CREATE_SUPPLIER", "suppliers",
                savedSupplier.getId().toString(),
                "Tạo mới nhà cung cấp: " + savedSupplier.getName());

        return convertToResponse(savedSupplier);
    }

    @Override
    public SupplierResponse updateSupplier(Integer id, SupplierSaveRequest request, User currentUser) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà cung cấp với ID: " + id));

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên nhà cung cấp không được để trống.");
        }

        supplier.setName(request.getName().trim());
        supplier.setContactInfo(request.getContactInfo() != null ? request.getContactInfo().trim() : null);
        supplier.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        if (request.getDebt() != null) {
            supplier.setDebt(request.getDebt());
        }

        Supplier updatedSupplier = supplierRepository.save(supplier);

        // Ghi audit log
        logAudit(currentUser.getId(), "UPDATE_SUPPLIER", "suppliers",
                id.toString(),
                "Cập nhật nhà cung cấp: " + updatedSupplier.getName());

        return convertToResponse(updatedSupplier);
    }

    @Override
    public void deleteSupplier(Integer id, User currentUser) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà cung cấp với ID: " + id));

        // Kiểm tra xem đối tác đã có giao dịch phát sinh chưa (phiếu kho liên kết)
        Number count = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM receipts WHERE supplier_id = ?")
                .setParameter(1, id)
                .getSingleResult();
        if (count != null && count.longValue() > 0) {
            throw new RuntimeException("Đối tác đã phát sinh giao dịch trong hệ thống, không thể xóa. Vui lòng chuyển trạng thái sang NGỪNG HOẠT ĐỘNG (INACTIVE).");
        }

        try {
            supplierRepository.delete(supplier);
            
            // Ghi audit log
            logAudit(currentUser.getId(), "DELETE_SUPPLIER", "suppliers",
                    id.toString(),
                    "Xóa nhà cung cấp: " + supplier.getName());
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("Đối tác đã phát sinh giao dịch trong hệ thống, không thể xóa. Vui lòng chuyển trạng thái sang NGỪNG HOẠT ĐỘNG (INACTIVE).");
        }
    }

    @Override
    public SupplierResponse toggleSupplierStatus(Integer id, User currentUser) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà cung cấp với ID: " + id));

        String newStatus = "ACTIVE".equals(supplier.getStatus()) ? "INACTIVE" : "ACTIVE";
        supplier.setStatus(newStatus);

        Supplier savedSupplier = supplierRepository.save(supplier);

        // Ghi audit log
        logAudit(currentUser.getId(), "TOGGLE_SUPPLIER_STATUS", "suppliers",
                id.toString(),
                "Chuyển trạng thái nhà cung cấp " + savedSupplier.getName() + " sang " + newStatus);

        return convertToResponse(savedSupplier);
    }

    @Override
    public SupplierResponse adjustDebt(Integer id, BigDecimal amount, User currentUser) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà cung cấp với ID: " + id));

        if (amount == null) {
            throw new RuntimeException("Số tiền điều chỉnh không được để trống.");
        }

        BigDecimal oldDebt = supplier.getDebt() != null ? supplier.getDebt() : BigDecimal.ZERO;
        BigDecimal newDebt = oldDebt.add(amount);
        supplier.setDebt(newDebt);

        Supplier savedSupplier = supplierRepository.save(supplier);

        // Ghi audit log
        logAudit(currentUser.getId(), "ADJUST_SUPPLIER_DEBT", "suppliers",
                id.toString(),
                "Điều chỉnh công nợ nhà cung cấp " + savedSupplier.getName() + ": " + oldDebt + " -> " + newDebt + " (Lượng đổi: " + amount + ")");

        return convertToResponse(savedSupplier);
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
            System.err.println("Audit Log insertion failed: " + e.getMessage());
        }
    }

    private SupplierResponse convertToResponse(Supplier s) {
        return new SupplierResponse(
                s.getId(),
                s.getName(),
                s.getContactInfo(),
                s.getAddress(),
                s.getDebt(),
                s.getStatus(),
                s.getCreatedAt()
        );
    }
}
