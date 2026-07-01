package com.example.Hehe.controller;

import com.example.Hehe.model.Backup;
import com.example.Hehe.model.User;
import com.example.Hehe.service.BackupService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    // 1. Tải file sao lưu JSON thủ công qua trình duyệt
    @GetMapping("/export")
    public ResponseEntity<?> exportBackup(@AuthenticationPrincipal User currentUser) {
        try {
            byte[] data = backupService.exportBranchData(currentUser);
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            // .wbk = WareHub Backup (AES-256-GCM encrypted binary)
            String filename = "backup_branch_" + currentUser.getBranch().getId() + "_" + dateStr + ".wbk";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 2. Tải lên file JSON để phục hồi dữ liệu chi nhánh
    @PostMapping("/import")
    public ResponseEntity<?> importBackup(@RequestParam("file") MultipartFile file,
                                          @AuthenticationPrincipal User currentUser) {
        try {
            backupService.importBranchData(file, currentUser);
            return ResponseEntity.ok(Map.of("message", "Phục hồi dữ liệu chi nhánh thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 3. Lấy danh sách lịch sử sao lưu lưu trên Server
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@AuthenticationPrincipal User currentUser) {
        try {
            List<Backup> history = backupService.getBackupHistory(currentUser);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 4. Khôi phục dữ liệu trực tiếp từ một bản ghi sao lưu trên Server
    @PostMapping("/restore/{id}")
    public ResponseEntity<?> restoreFromHistory(@PathVariable Integer id,
                                                @AuthenticationPrincipal User currentUser) {
        try {
            backupService.restoreFromHistory(id, currentUser);
            return ResponseEntity.ok(Map.of("message", "Phục hồi dữ liệu từ bản sao lưu thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 5. Xóa tệp tin sao lưu trên Server
    @DeleteMapping("/history/{id}")
    public ResponseEntity<?> deleteBackup(@PathVariable Integer id,
                                          @AuthenticationPrincipal User currentUser) {
        try {
            backupService.deleteBackup(id, currentUser);
            return ResponseEntity.ok(Map.of("message", "Đã xóa tệp tin sao lưu thành công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 6. Trigger auto backup thủ công (ADMIN only)
    @PostMapping("/trigger-scheduled")
    public ResponseEntity<?> triggerScheduledBackup(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getRole() != com.example.Hehe.model.UserRole.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("message", "Chỉ ADMIN mới có quyền trigger tác vụ này."));
        }
        try {
            backupService.performScheduledBackup();
            return ResponseEntity.ok(Map.of("message", "Đã chạy sao lưu tự động cho tất cả chi nhánh!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ADMIN: System Config Backup/Restore
    // ═══════════════════════════════════════════════════════════════

    // 7. Tải file System Config Backup (.wbk)
    @GetMapping("/system/export")
    public ResponseEntity<?> exportSystemConfig(@AuthenticationPrincipal User currentUser) {
        try {
            byte[] data = backupService.exportSystemConfig(currentUser);
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "system_config_" + dateStr + ".wbk";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 8. Upload file để phục hồi System Config
    @PostMapping("/system/import")
    public ResponseEntity<?> importSystemConfig(@RequestParam("file") MultipartFile file,
                                                @AuthenticationPrincipal User currentUser) {
        try {
            backupService.importSystemConfig(file, currentUser);
            return ResponseEntity.ok(Map.of("message", "Phục hồi cấu hình hệ thống thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 9. Lịch sử System Config Backup trên server
    @GetMapping("/system/history")
    public ResponseEntity<?> getSystemBackupHistory(@AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(backupService.getSystemBackupHistory(currentUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 10. Khôi phục System Config từ bản trên server
    @PostMapping("/system/restore/{id}")
    public ResponseEntity<?> restoreSystemFromHistory(@PathVariable Integer id,
                                                      @AuthenticationPrincipal User currentUser) {
        try {
            backupService.restoreSystemFromHistory(id, currentUser);
            return ResponseEntity.ok(Map.of("message", "Phục hồi cấu hình hệ thống thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 11. Xóa file System Config Backup
    @DeleteMapping("/system/history/{id}")
    public ResponseEntity<?> deleteSystemBackup(@PathVariable Integer id,
                                                @AuthenticationPrincipal User currentUser) {
        try {
            backupService.deleteSystemBackup(id, currentUser);
            return ResponseEntity.ok(Map.of("message", "Đã xóa bản sao lưu hệ thống."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
