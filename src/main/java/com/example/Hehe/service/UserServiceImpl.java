package com.example.Hehe.service;

import com.example.Hehe.dto.ChangePasswordRequest;
import com.example.Hehe.dto.UserResponse;
import com.example.Hehe.dto.UserSaveRequest;
import com.example.Hehe.model.Branch;
import com.example.Hehe.model.User;
import com.example.Hehe.model.UserRole;
import com.example.Hehe.model.UserStatus;
import com.example.Hehe.repository.BranchRepository;
import com.example.Hehe.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserServiceImpl(UserRepository userRepository, BranchRepository branchRepository,
                            PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String keyword, String role, Integer branchId, String status, User currentUser) {
        // STAFF không có quyền truy cập
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Bạn không có quyền truy cập chức năng này.");
        }

        // Đọc các giá trị bộ lọc dạng Enum
        UserRole filterRole = (role != null && !role.trim().isEmpty()) ? UserRole.valueOf(role.toUpperCase()) : null;
        UserStatus filterStatus = (status != null && !status.trim().isEmpty()) ? UserStatus.valueOf(status.toUpperCase()) : null;
        String pattern = (keyword != null && !keyword.trim().isEmpty()) ? "%" + keyword.trim().toLowerCase() + "%" : null;

        // Tìm chi nhánh tổng (dùng làm fallback nếu tài khoản chưa có chi nhánh)
        Branch headBranch = branchRepository.findByIsHeadTrue().stream()
                .findFirst()
                .orElseGet(() -> branchRepository.findAll().stream()
                        .min(java.util.Comparator.comparing(Branch::getId))
                        .orElse(null));
        final Integer headBranchId = headBranch != null ? headBranch.getId() : null;

        // Xác định chi nhánh của người đang đăng nhập
        final Integer isolatedBranchId = currentUser.getBranch() != null ? currentUser.getBranch().getId() : headBranchId;

        // Lấy tất cả user thoả mãn từ khoá/role/status (bỏ qua branchId từ query UI)
        List<User> users = userRepository.searchUsers(pattern, filterRole, null, filterStatus);

        // Cách ly dữ liệu hoàn toàn bằng Java Stream:
        // Chỉ giữ lại những người dùng có branchId trùng với isolatedBranchId của người đang đăng nhập.
        // Những user nào trong DB chưa có branch (như tài khoản admin cũ), ta quy ước họ thuộc chi nhánh tổng.
        users = users.stream().filter(u -> {
            Integer targetUserBranchId = u.getBranch() != null ? u.getBranch().getId() : headBranchId;
            return isolatedBranchId != null && isolatedBranchId.equals(targetUserBranchId);
        }).collect(Collectors.toList());

        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse createUser(UserSaveRequest request, User currentUser) {
        // STAFF không có quyền tạo mới
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Bạn không có quyền thực hiện thao tác này.");
        }

        // Validate dữ liệu bắt buộc
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Tên đăng nhập không được để trống.");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu không được để trống.");
        }
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new RuntimeException("Họ và tên không được để trống.");
        }

        // Kiểm tra trùng username
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new RuntimeException("Tên đăng nhập '" + request.getUsername().trim() + "' đã tồn tại.");
        }

        // Kiểm tra định dạng email (nếu gửi lên)
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String email = request.getEmail().trim();
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new RuntimeException("Định dạng email không hợp lệ.");
            }
        }

        // Kiểm tra định dạng số điện thoại (nếu gửi lên)
        String cleanPhone = null;
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            cleanPhone = request.getPhone().trim().replaceAll("[-. ]", "");
            if (!cleanPhone.matches("^(0|\\+84|84)[0-9]{9,11}$")) {
                throw new RuntimeException("Số điện thoại không hợp lệ (phải bắt đầu bằng 0, 84 hoặc +84 và gồm 10-12 chữ số).");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setFullName(request.getFullName().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        user.setPhone(cleanPhone);
        user.setStatus(UserStatus.ACTIVE);

        // Phân quyền tạo mới
        if (currentUser.getRole() == UserRole.MANAGER) {
            // MANAGER chỉ được tạo STAFF thuộc chi nhánh của mình
            user.setRole(UserRole.STAFF);
            user.setBranch(currentUser.getBranch());
        } else {
            // ADMIN tạo theo quy định nghiệp vụ
            UserRole targetRole = UserRole.valueOf(request.getRole().toUpperCase());

            Branch branch = null;
            if (request.getBranchId() != null) {
                branch = branchRepository.findById(request.getBranchId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh với ID: " + request.getBranchId()));
            }

            // Tìm chi nhánh tổng
            Branch headBranch = branchRepository.findByIsHeadTrue().stream()
                    .findFirst()
                    .orElseGet(() -> branchRepository.findAll().stream()
                            .min(java.util.Comparator.comparing(Branch::getId))
                            .orElse(null));

            final Integer headBranchId = headBranch != null ? headBranch.getId() : null;

            // Ràng buộc vai trò theo chi nhánh:
            if (branch != null) {
                if (branch.getId().equals(headBranchId)) {
                    // Chi nhánh tổng: chỉ cho phép ADMIN hoặc STAFF
                    if (targetRole == UserRole.MANAGER) {
                        throw new RuntimeException("Chi nhánh tổng không có vai trò MANAGER. Vui lòng chọn ADMIN hoặc STAFF.");
                    }
                } else {
                    // Chi nhánh con: chỉ cho phép MANAGER
                    if (targetRole != UserRole.MANAGER) {
                        throw new RuntimeException("Tại chi nhánh con, bạn chỉ được phép tạo tài khoản quản lý (MANAGER).");
                    }
                }
            } else {
                // Không chọn chi nhánh -> Bắt buộc là ADMIN
                if (targetRole != UserRole.ADMIN) {
                    throw new RuntimeException("Vui lòng chọn chi nhánh cho người dùng này.");
                }
            }

            user.setRole(targetRole);
            user.setBranch(branch);
        }

        user.setUpdatedAt(java.time.LocalDateTime.now());
        User savedUser = userRepository.save(user);

        // Ghi Nhật ký
        auditLogService.logAction(currentUser, "CREATE", "users",
                String.valueOf(savedUser.getId()),
                "Tạo tài khoản mới: " + savedUser.getFullName() + " (" + savedUser.getUsername() + ") - Vai trò: " + savedUser.getRole().name());

        return convertToResponse(savedUser);
    }

    @Override
    public UserResponse updateUser(Integer id, UserSaveRequest request, User currentUser) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Bạn không có quyền thực hiện thao tác này.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

        // Kiểm tra quyền sửa của MANAGER
        if (currentUser.getRole() == UserRole.MANAGER) {
            if (user.getRole() != UserRole.STAFF || user.getBranch() == null || 
                !user.getBranch().getId().equals(currentUser.getBranch().getId())) {
                throw new RuntimeException("Bạn chỉ có quyền chỉnh sửa nhân viên cùng chi nhánh.");
            }
        }

        // Sửa Username: kiểm tra trùng lặp
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty() && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername().trim())) {
                throw new RuntimeException("Tên đăng nhập '" + request.getUsername().trim() + "' đã tồn tại.");
            }
            user.setUsername(request.getUsername().trim());
        }

        // Sửa Email: kiểm tra định dạng
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String email = request.getEmail().trim();
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new RuntimeException("Định dạng email không hợp lệ.");
            }
            user.setEmail(email);
        } else {
            user.setEmail(null);
        }

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        // Sửa Số điện thoại: kiểm tra định dạng
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            String phone = request.getPhone().trim().replaceAll("[-. ]", "");
            if (!phone.matches("^(0|\\+84|84)[0-9]{9,11}$")) {
                throw new RuntimeException("Số điện thoại không hợp lệ (phải bắt đầu bằng 0, 84 hoặc +84 và gồm 10-12 chữ số).");
            }
            user.setPhone(phone);
        } else {
            user.setPhone(null);
        }

        // Cập nhật Mật khẩu (nếu nhập mới)
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Cập nhật Vai trò & Chi nhánh (Chỉ ADMIN mới được đổi)
        if (currentUser.getRole() == UserRole.ADMIN) {
            // Không được tự hạ quyền ADMIN của chính mình
            if (currentUser.getId().equals(id) && user.getRole() == UserRole.ADMIN && !request.getRole().toUpperCase().equals("ADMIN")) {
                throw new RuntimeException("Bạn không được tự hạ quyền ADMIN của chính mình.");
            }

            UserRole targetRole = UserRole.valueOf(request.getRole().toUpperCase());
            
            Branch branch = null;
            if (request.getBranchId() != null) {
                branch = branchRepository.findById(request.getBranchId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh với ID: " + request.getBranchId()));
            }

            // Tìm chi nhánh tổng
            Branch headBranch = branchRepository.findByIsHeadTrue().stream()
                    .findFirst()
                    .orElseGet(() -> branchRepository.findAll().stream()
                            .min(java.util.Comparator.comparing(Branch::getId))
                            .orElse(null));

            final Integer headBranchId = headBranch != null ? headBranch.getId() : null;

            // Ràng buộc vai trò theo chi nhánh:
            if (branch != null) {
                if (branch.getId().equals(headBranchId)) {
                    if (targetRole == UserRole.MANAGER) {
                        throw new RuntimeException("Chi nhánh tổng không có vai trò MANAGER. Vui lòng chọn ADMIN hoặc STAFF.");
                    }
                } else {
                    if (targetRole != UserRole.MANAGER) {
                        throw new RuntimeException("Tại chi nhánh con, bạn chỉ được phép thiết lập vai trò quản lý (MANAGER).");
                    }
                }
            } else {
                if (targetRole != UserRole.ADMIN) {
                    throw new RuntimeException("Vui lòng chọn chi nhánh.");
                }
            }

            user.setRole(targetRole);
            user.setBranch(branch);
        }

        user.setUpdatedAt(java.time.LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        // Ghi Nhật ký
        auditLogService.logAction(currentUser, "UPDATE", "users",
                String.valueOf(updatedUser.getId()),
                "Cập nhật thông tin tài khoản: " + updatedUser.getFullName() + " (" + updatedUser.getUsername() + ")");

        return convertToResponse(updatedUser);
    }

    @Override
    public void deleteUser(Integer id, User currentUser) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Bạn không có quyền thực hiện thao tác này.");
        }

        // Chặn tự xóa chính mình
        if (currentUser.getId().equals(id)) {
            throw new RuntimeException("Bạn không được tự xóa tài khoản của chính mình.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

        // MANAGER chỉ được xóa STAFF cùng chi nhánh
        if (currentUser.getRole() == UserRole.MANAGER) {
            if (user.getRole() != UserRole.STAFF || user.getBranch() == null || 
                !user.getBranch().getId().equals(currentUser.getBranch().getId())) {
                throw new RuntimeException("Bạn chỉ có quyền xóa nhân viên cùng chi nhánh.");
            }
        }

        try {
            String fullName = user.getFullName();
            String username = user.getUsername();
            userRepository.delete(user);

            // Ghi Nhật ký
            auditLogService.logAction(currentUser, "DELETE", "users",
                    String.valueOf(id),
                    "Xóa tài khoản: " + fullName + " (" + username + ")");
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("Tài khoản đã phát sinh dữ liệu giao dịch trong hệ thống, không thể xóa. Vui lòng sử dụng tính năng khóa tài khoản.");
        }
    }

    @Override
    public UserResponse toggleUserStatus(Integer id, User currentUser) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Bạn không có quyền thực hiện thao tác này.");
        }

        // Chặn tự khóa chính mình
        if (currentUser.getId().equals(id)) {
            throw new RuntimeException("Bạn không thể tự khóa tài khoản của chính mình.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

        // MANAGER chỉ được khóa STAFF cùng chi nhánh
        if (currentUser.getRole() == UserRole.MANAGER) {
            if (user.getRole() != UserRole.STAFF || user.getBranch() == null || 
                !user.getBranch().getId().equals(currentUser.getBranch().getId())) {
                throw new RuntimeException("Bạn chỉ có quyền khóa/mở khóa nhân viên cùng chi nhánh.");
            }
        }

        // Thay đổi trạng thái
        boolean isLocking = user.getStatus() == UserStatus.ACTIVE;
        if (isLocking) {
            user.setStatus(UserStatus.LOCKED);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }

        user.setUpdatedAt(java.time.LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        // Ghi Nhật ký
        String actionLabel = isLocking ? "LOCK_ACCOUNT" : "UPDATE";
        String desc = isLocking
                ? "Khóa tài khoản: " + updatedUser.getFullName() + " (" + updatedUser.getUsername() + ")"
                : "Mở khóa tài khoản: " + updatedUser.getFullName() + " (" + updatedUser.getUsername() + ")";
        auditLogService.logAction(currentUser, actionLabel, "users",
                String.valueOf(updatedUser.getId()), desc);

        return convertToResponse(updatedUser);
    }

    @Override
    public void changePassword(ChangePasswordRequest request, User currentUser) {
        // Validate dữ liệu đầu vào
        if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập mật khẩu hiện tại.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập mật khẩu mới.");
        }
        if (request.getNewPassword().length() < 8) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 8 ký tự.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp với mật khẩu mới.");
        }

        // Load user mới nhất từ DB
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không chính xác.");
        }

        // Hash và lưu mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
    private UserResponse convertToResponse(User user) {
        Integer branchId = user.getBranch() != null ? user.getBranch().getId() : null;
        String branchName = user.getBranch() != null ? user.getBranch().getName() : null;
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                branchId,
                branchName,
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getPhone()
        );
    }
}
