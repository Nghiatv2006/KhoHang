package com.example.Hehe.service;

import com.example.Hehe.dto.TransferRequestResponse;
import com.example.Hehe.dto.TransferRequestSaveRequest;
import com.example.Hehe.model.Branch;
import com.example.Hehe.model.BranchTransferRequest;
import com.example.Hehe.model.User;
import com.example.Hehe.model.UserRole;
import com.example.Hehe.repository.BranchRepository;
import com.example.Hehe.repository.BranchTransferRequestRepository;
import com.example.Hehe.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BranchTransferRequestServiceImpl implements BranchTransferRequestService {

    private final BranchTransferRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public BranchTransferRequestServiceImpl(BranchTransferRequestRepository requestRepository,
                                             UserRepository userRepository,
                                             BranchRepository branchRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
    }

    @Override
    public TransferRequestResponse createRequest(TransferRequestSaveRequest request, User currentUser) {
        // Chỉ MANAGER mới được gửi yêu cầu
        if (currentUser.getRole() != UserRole.MANAGER) {
            throw new RuntimeException("Chỉ tài khoản Quản lý (MANAGER) mới được phép gửi yêu cầu chuyển chi nhánh.");
        }

        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + request.getStaffId()));

        // Kiểm tra xem nhân viên đó có đúng là STAFF thuộc chi nhánh của MANAGER này không
        if (staff.getRole() != UserRole.STAFF || staff.getBranch() == null ||
            !staff.getBranch().getId().equals(currentUser.getBranch().getId())) {
            throw new RuntimeException("Bạn chỉ được gửi yêu cầu chuyển chi nhánh cho nhân viên thuộc chi nhánh của mình.");
        }

        Branch toBranch = branchRepository.findById(request.getToBranchId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh nhận với ID: " + request.getToBranchId()));

        // Chi nhánh mới phải khác chi nhánh hiện tại
        if (toBranch.getId().equals(staff.getBranch().getId())) {
            throw new RuntimeException("Chi nhánh nhận phải khác chi nhánh hiện tại của nhân viên.");
        }

        // Kiểm tra xem đã có yêu cầu PENDING nào cho STAFF này chưa
        if (requestRepository.existsByStaffAndStatus(staff, "PENDING")) {
            throw new RuntimeException("Nhân viên này đang có một yêu cầu chuyển chi nhánh đang chờ duyệt.");
        }

        BranchTransferRequest transferRequest = new BranchTransferRequest();
        transferRequest.setStaff(staff);
        transferRequest.setFromBranch(staff.getBranch());
        transferRequest.setToBranch(toBranch);
        transferRequest.setCreatedBy(currentUser);
        transferRequest.setStatus("PENDING");

        BranchTransferRequest savedRequest = requestRepository.save(transferRequest);

        // Ghi nhận nhật ký audit log
        logAudit(currentUser.getId(), "CREATE_TRANSFER_REQUEST", "branch_transfer_requests",
                savedRequest.getId().toString(),
                "Yêu cầu chuyển nhân viên " + staff.getFullName() + " từ chi nhánh " +
                        transferRequest.getFromBranch().getName() + " sang " + toBranch.getName());

        return convertToResponse(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferRequestResponse> getRequests(User currentUser) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Bạn không có quyền truy cập chức năng này.");
        }

        List<BranchTransferRequest> requests;
        if (currentUser.getRole() == UserRole.ADMIN) {
            requests = requestRepository.findAll();
        } else {
            requests = requestRepository.findByCreatedBy(currentUser);
        }

        return requests.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TransferRequestResponse approveRequest(Integer requestId, User currentUser) {
        // Chỉ ADMIN mới được duyệt
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Chỉ tài khoản Quản trị viên (ADMIN) mới được quyền duyệt yêu cầu.");
        }

        BranchTransferRequest transferRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu chuyển chi nhánh với ID: " + requestId));

        if (!"PENDING".equals(transferRequest.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý (trạng thái hiện tại: " + transferRequest.getStatus() + ").");
        }

        User staff = transferRequest.getStaff();
        Branch toBranch = transferRequest.getToBranch();

        // 1. Thực hiện chuyển chi nhánh cho STAFF
        staff.setBranch(toBranch);
        userRepository.save(staff);

        // 2. Cập nhật trạng thái yêu cầu
        transferRequest.setStatus("APPROVED");
        transferRequest.setApprovedBy(currentUser);
        transferRequest.setApprovedAt(LocalDateTime.now());
        BranchTransferRequest approvedRequest = requestRepository.save(transferRequest);

        // 3. Ghi audit log
        logAudit(currentUser.getId(), "APPROVE_TRANSFER_REQUEST", "branch_transfer_requests",
                requestId.toString(),
                "Phê duyệt chuyển nhân viên " + staff.getFullName() + " sang chi nhánh " + toBranch.getName());

        return convertToResponse(approvedRequest);
    }

    @Override
    public TransferRequestResponse rejectRequest(Integer requestId, User currentUser) {
        // Chỉ ADMIN mới được từ chối
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Chỉ tài khoản Quản trị viên (ADMIN) mới được quyền từ chối yêu cầu.");
        }

        BranchTransferRequest transferRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu chuyển chi nhánh với ID: " + requestId));

        if (!"PENDING".equals(transferRequest.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý (trạng thái hiện tại: " + transferRequest.getStatus() + ").");
        }

        // 1. Cập nhật trạng thái yêu cầu từ chối
        transferRequest.setStatus("REJECTED");
        transferRequest.setApprovedBy(currentUser);
        transferRequest.setApprovedAt(LocalDateTime.now());
        BranchTransferRequest rejectedRequest = requestRepository.save(transferRequest);

        // 2. Ghi audit log
        logAudit(currentUser.getId(), "REJECT_TRANSFER_REQUEST", "branch_transfer_requests",
                requestId.toString(),
                "Từ chối chuyển nhân viên " + transferRequest.getStaff().getFullName());

        return convertToResponse(rejectedRequest);
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

    private TransferRequestResponse convertToResponse(BranchTransferRequest req) {
        Integer approvedById = req.getApprovedBy() != null ? req.getApprovedBy().getId() : null;
        String approvedByName = req.getApprovedBy() != null ? req.getApprovedBy().getFullName() : null;

        return new TransferRequestResponse(
                req.getId(),
                req.getStaff().getId(),
                req.getStaff().getFullName(),
                req.getFromBranch().getId(),
                req.getFromBranch().getName(),
                req.getToBranch().getId(),
                req.getToBranch().getName(),
                req.getCreatedBy().getId(),
                req.getCreatedBy().getFullName(),
                req.getStatus(),
                req.getCreatedAt(),
                approvedById,
                approvedByName,
                req.getApprovedAt()
        );
    }
}
