package com.example.Hehe.security;

import com.example.Hehe.model.Branch;
import com.example.Hehe.model.User;
import com.example.Hehe.repository.BranchRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class BranchLockInterceptor implements HandlerInterceptor {

    private final BranchRepository branchRepository;

    public BranchLockInterceptor(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 1. Ngoại trừ các API phục hồi dữ liệu hoặc đăng nhập/đăng xuất/lấy thông tin cá nhân
        if (uri.startsWith("/api/backup/import") ||
            uri.startsWith("/api/backup/restore") ||
            uri.startsWith("/api/auth") ||
            uri.startsWith("/api/users/me")) {
            return true;
        }

        // 2. Lấy thông tin người dùng đang kết nối
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            User currentUser = (User) auth.getPrincipal();
            if (currentUser.getBranch() != null) {
                Integer branchId = currentUser.getBranch().getId();
                
                // 3. Truy vấn trực tiếp DB để lấy trạng thái khóa mới nhất
                Branch branch = branchRepository.findById(branchId).orElse(null);
                if (branch != null && Boolean.TRUE.equals(branch.getIsLocked())) {
                    // Trả về HTTP 423 Locked
                    response.setStatus(423);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"message\": \"Chi nhánh đang trong quá trình bảo trì hoặc phục hồi dữ liệu, vui lòng quay lại sau.\"}");
                    return false;
                }
            }
        }

        return true;
    }
}
