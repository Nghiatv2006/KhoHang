package com.example.Hehe.service;

import com.example.Hehe.dto.CustomerResponse;
import com.example.Hehe.dto.CustomerSaveRequest;
import com.example.Hehe.model.Customer;
import com.example.Hehe.model.User;
import com.example.Hehe.repository.CustomerRepository;
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
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> searchCustomers(String keyword, String status, User currentUser) {
        String pattern = (keyword != null && !keyword.trim().isEmpty()) ? "%" + keyword.trim().toLowerCase() + "%" : null;
        String filterStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : null;

        return customerRepository.searchCustomers(pattern, filterStatus)
                .stream()
                .filter(c -> {
                    if (currentUser.getRole() == com.example.Hehe.model.UserRole.ADMIN) return true;
                    return currentUser.getBranch() != null && c.getBranch() != null && c.getBranch().getId().equals(currentUser.getBranch().getId());
                })
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));
        return convertToResponse(customer);
    }

    @Override
    public CustomerResponse createCustomer(CustomerSaveRequest request, User currentUser) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên khách hàng không được để trống.");
        }

        Customer customer = new Customer();
        customer.setName(request.getName().trim());
        customer.setContactInfo(request.getContactInfo() != null ? request.getContactInfo().trim() : null);
        customer.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        customer.setDebt(request.getDebt() != null ? request.getDebt() : BigDecimal.ZERO);
        customer.setStatus("ACTIVE");
        customer.setBranch(currentUser.getBranch());

        Customer savedCustomer = customerRepository.save(customer);

        // Ghi audit log
        logAudit(currentUser.getId(), "CREATE_CUSTOMER", "customers",
                savedCustomer.getId().toString(),
                "Tạo mới khách hàng: " + savedCustomer.getName());

        return convertToResponse(savedCustomer);
    }

    @Override
    public CustomerResponse updateCustomer(Integer id, CustomerSaveRequest request, User currentUser) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên khách hàng không được để trống.");
        }

        customer.setName(request.getName().trim());
        customer.setContactInfo(request.getContactInfo() != null ? request.getContactInfo().trim() : null);
        customer.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        if (request.getDebt() != null) {
            customer.setDebt(request.getDebt());
        }

        Customer updatedCustomer = customerRepository.save(customer);

        // Ghi audit log
        logAudit(currentUser.getId(), "UPDATE_CUSTOMER", "customers",
                id.toString(),
                "Cập nhật khách hàng: " + updatedCustomer.getName());

        return convertToResponse(updatedCustomer);
    }

    @Override
    public void deleteCustomer(Integer id, User currentUser) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));

        // Kiểm tra xem đối tác đã có giao dịch phát sinh chưa (phiếu kho liên kết)
        Number count = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM receipts WHERE customer_id = ?")
                .setParameter(1, id)
                .getSingleResult();
        if (count != null && count.longValue() > 0) {
            throw new RuntimeException("Đối tác đã phát sinh giao dịch trong hệ thống, không thể xóa. Vui lòng chuyển trạng thái sang NGỪNG HOẠT ĐỘNG (INACTIVE).");
        }

        try {
            customerRepository.delete(customer);
            
            // Ghi audit log
            logAudit(currentUser.getId(), "DELETE_CUSTOMER", "customers",
                    id.toString(),
                    "Xóa khách hàng: " + customer.getName());
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("Đối tác đã phát sinh giao dịch trong hệ thống, không thể xóa. Vui lòng chuyển trạng thái sang NGỪNG HOẠT ĐỘNG (INACTIVE).");
        }
    }

    @Override
    public CustomerResponse toggleCustomerStatus(Integer id, User currentUser) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));

        String newStatus = "ACTIVE".equals(customer.getStatus()) ? "INACTIVE" : "ACTIVE";
        customer.setStatus(newStatus);

        Customer savedCustomer = customerRepository.save(customer);

        // Ghi audit log
        logAudit(currentUser.getId(), "TOGGLE_CUSTOMER_STATUS", "customers",
                id.toString(),
                "Chuyển trạng thái khách hàng " + savedCustomer.getName() + " sang " + newStatus);

        return convertToResponse(savedCustomer);
    }

    @Override
    public CustomerResponse adjustDebt(Integer id, BigDecimal amount, User currentUser) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));

        if (amount == null) {
            throw new RuntimeException("Số tiền điều chỉnh không được để trống.");
        }

        BigDecimal oldDebt = customer.getDebt() != null ? customer.getDebt() : BigDecimal.ZERO;
        
        if (amount.compareTo(BigDecimal.ZERO) < 0 && amount.abs().compareTo(oldDebt) > 0) {
            throw new RuntimeException("Số tiền giảm nợ không được vượt quá số nợ hiện tại (" + oldDebt + " VNĐ).");
        }
        
        BigDecimal newDebt = oldDebt.add(amount);

        if (newDebt.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Công nợ không được điều chỉnh xuống mức âm (Số tiền giảm tối đa là " + oldDebt + " VNĐ).");
        }

        customer.setDebt(newDebt);

        Customer savedCustomer = customerRepository.save(customer);

        // Ghi audit log
        logAudit(currentUser.getId(), "ADJUST_CUSTOMER_DEBT", "customers",
                id.toString(),
                "Điều chỉnh công nợ khách hàng " + savedCustomer.getName() + ": " + oldDebt + " -> " + newDebt + " (Lượng đổi: " + amount + ")");

        return convertToResponse(savedCustomer);
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

    private CustomerResponse convertToResponse(Customer c) {
        return new CustomerResponse(
                c.getId(),
                c.getName(),
                c.getContactInfo(),
                c.getAddress(),
                c.getDebt(),
                c.getStatus(),
                c.getCreatedAt()
        );
    }
}
