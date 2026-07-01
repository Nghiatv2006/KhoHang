package com.example.Hehe.service;

import com.example.Hehe.model.Backup;
import com.example.Hehe.model.User;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface BackupService {

    // ── Chi nhánh (MANAGER) ───────────────────────────────────────────────────

    // Xuất dữ liệu chi nhánh dưới dạng file .wbk (AES-256-GCM + HMAC-SHA256)
    byte[] exportBranchData(User currentUser);

    // Nhập dữ liệu chi nhánh từ file .wbk tải lên (kiểm tra + giải mã)
    void importBranchData(MultipartFile file, User currentUser);

    // Lấy lịch sử các tệp tin sao lưu chi nhánh trên server
    List<Backup> getBackupHistory(User currentUser);

    // Khôi phục dữ liệu chi nhánh từ bản sao lưu trên server
    void restoreFromHistory(Integer backupId, User currentUser);

    // Xóa file sao lưu chi nhánh trên server
    void deleteBackup(Integer backupId, User currentUser);

    // Tiến trình sao lưu tự động định kỳ (tất cả chi nhánh)
    void performScheduledBackup();

    // ── Hệ thống (ADMIN) ──────────────────────────────────────────────────────

    // Xuất cấu hình hệ thống (branches, categories, products, admin users)
    byte[] exportSystemConfig(User currentUser);

    // Nhập cấu hình hệ thống từ file .wbk tải lên
    void importSystemConfig(MultipartFile file, User currentUser);

    // Lấy lịch sử sao lưu cấu hình hệ thống
    List<Backup> getSystemBackupHistory(User currentUser);

    // Khôi phục cấu hình hệ thống từ bản sao lưu trên server
    void restoreSystemFromHistory(Integer backupId, User currentUser);

    // Xóa file sao lưu cấu hình hệ thống
    void deleteSystemBackup(Integer backupId, User currentUser);
}
