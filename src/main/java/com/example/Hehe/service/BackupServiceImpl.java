package com.example.Hehe.service;

import com.example.Hehe.model.Backup;
import com.example.Hehe.model.Branch;
import com.example.Hehe.model.User;
import com.example.Hehe.model.UserRole;
import com.example.Hehe.repository.BackupRepository;
import com.example.Hehe.repository.BranchRepository;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Service
public class BackupServiceImpl implements BackupService {

    private final BackupRepository backupRepository;
    private final BranchRepository branchRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final BranchLockHelper branchLockHelper;
    private final TransactionTemplate transactionTemplate;
    private final AuditLogService auditLogService;

    @Value("${app.backup.secret}")
    private String secretKey;

    private static final String BACKUP_DIR = "uploads/backups/";
    private static final String MANUAL_BACKUP_SUBDIR = "manual_branch_";
    private static final String AUTO_BACKUP_SUBDIR   = "auto_branch_";

    // ─── AES-256-GCM Encryption constants ────────────────────────────────────
    /** Magic header để nhận diện file đã mã hoá (4 bytes ASCII). */
    private static final byte[] MAGIC_HEADER = {0x57, 0x48, 0x42, 0x4B}; // "WHBK"
    /** Version 1: AES-GCM thuần (không nén). */
    private static final byte  ENC_VERSION_V1 = 0x01;
    /** Version 2: GZIP + AES-GCM (mặc định từ bản nâng cấp). */
    private static final byte  ENC_VERSION_V2 = 0x02;
    /** Kích thước IV cho AES-GCM (chuẩn 96-bit = 12 bytes). */
    private static final int   GCM_IV_SIZE   = 12;
    /** Kích thước authentication tag của GCM (128-bit = 16 bytes). */
    private static final int   GCM_TAG_BITS  = 128;
    /** Kích thước buffer cho streaming (8 KB). */
    private static final int   STREAM_BUFFER_SIZE = 8192;

    public BackupServiceImpl(BackupRepository backupRepository,
                             BranchRepository branchRepository,
                             JdbcTemplate jdbcTemplate,
                             BranchLockHelper branchLockHelper,
                             TransactionTemplate transactionTemplate,
                             AuditLogService auditLogService) {
        this.backupRepository    = backupRepository;
        this.branchRepository    = branchRepository;
        this.jdbcTemplate        = jdbcTemplate;
        this.branchLockHelper    = branchLockHelper;
        this.transactionTemplate = transactionTemplate;
        this.auditLogService     = auditLogService;

        // Cấu hình ObjectMapper riêng: thứ tự key cố định để HMAC nhất quán
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fix 5: Reset stale locks khi server khởi động lại
    //        Nếu JVM crash giữa chừng, chi nhánh có thể bị kẹt is_locked=true.
    //        ApplicationReadyEvent đảm bảo Spring context + DB sẵn sàng.
    // ─────────────────────────────────────────────────────────────────────────
    @EventListener(ApplicationReadyEvent.class)
    public void resetStaleLocks() {
        int count = jdbcTemplate.update("UPDATE branches SET is_locked = false WHERE is_locked = true");
        if (count > 0) {
            System.out.println("[BackupService] WARNING: Đã reset " + count
                    + " chi nhánh bị kẹt trạng thái is_locked=true từ lần chạy trước.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. SAO LƯU THỦ CÔNG — trả về byte[] để trình duyệt download
    //    Fix 1: Lưu file vào server + ghi record vào bảng backups
    //    Fix 6: Ghi audit log
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public byte[] exportBranchData(User currentUser) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Nhân viên không có quyền thực hiện thao tác này.");
        }
        if (currentUser.getBranch() == null) {
            throw new RuntimeException("Tài khoản chưa được phân công vào chi nhánh nào.");
        }

        Integer branchId = currentUser.getBranch().getId();

        // Tạo JSON → ký HMAC → mã hoá AES-256-GCM
        byte[] plainJson  = generateBackupJson(branchId);
        byte[] encrypted  = encryptData(plainJson);

        // Fix 1: Lưu file thủ công lên server + ghi record vào DB
        try {
            String dateStr   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            // Đuôi .wbk (WareHub Backup) — phân biệt với file JSON thuần
            String filename  = "manual_backup_branch_" + branchId + "_" + dateStr + ".wbk";
            Path   branchDir = Paths.get(BACKUP_DIR + MANUAL_BACKUP_SUBDIR + branchId);
            Files.createDirectories(branchDir);
            Path filePath = branchDir.resolve(filename);
            Files.write(filePath, encrypted);

            Backup record = new Backup();
            record.setBranch(currentUser.getBranch());
            record.setFilename(filename);
            record.setFilepath(filePath.toString());
            record.setFileSize((long) encrypted.length);
            record.setBackupType("MANUAL");
            record.setCreatedBy(currentUser);
            backupRepository.save(record);

            // Fix 6: Ghi audit log
            auditLogService.logAction(currentUser, "BACKUP", "backups", String.valueOf(record.getId()),
                    "Đã tạo bản sao lưu thủ công (AES-256-GCM): " + filename
                            + " | Kích thước: " + formatBytes(encrypted.length));

        } catch (IOException e) {
            System.err.println("[BackupService] Lỗi khi lưu file backup thủ công lên server: " + e.getMessage());
        }

        return encrypted;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. PHỤC HỒI — Upload file từ máy tính
    //    Fix 3: BranchLockHelper + TransactionTemplate
    //    Fix 6: Ghi audit log
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void importBranchData(MultipartFile file, User currentUser) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Nhân viên không có quyền thực hiện thao tác này.");
        }
        if (currentUser.getBranch() == null) {
            throw new RuntimeException("Tài khoản chưa được phân công vào chi nhánh nào.");
        }

        byte[] rawBytes;
        try {
            rawBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc tệp tin tải lên: " + e.getMessage());
        }

        restoreFromBytes(rawBytes, currentUser.getBranch().getId(), currentUser.getId(), currentUser);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Lấy lịch sử backup của chi nhánh
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<Backup> getBackupHistory(User currentUser) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Nhân viên không có quyền thực hiện thao tác này.");
        }
        if (currentUser.getBranch() == null) {
            throw new RuntimeException("Tài khoản chưa được phân công vào chi nhánh nào.");
        }
        return backupRepository.findByBranchIdOrderByCreatedAtDesc(currentUser.getBranch().getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. PHỤC HỒI — Từ bản backup đã lưu trên server
    //    Fix 3: BranchLockHelper + TransactionTemplate
    //    Fix 6: Ghi audit log
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void restoreFromHistory(Integer backupId, User currentUser) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Nhân viên không có quyền thực hiện thao tác này.");
        }
        if (currentUser.getBranch() == null) {
            throw new RuntimeException("Tài khoản chưa được phân công vào chi nhánh nào.");
        }

        Backup backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản sao lưu với ID: " + backupId));

        if (!backup.getBranch().getId().equals(currentUser.getBranch().getId())) {
            throw new RuntimeException("Bản sao lưu này thuộc chi nhánh khác.");
        }

        Path filePath = Paths.get(backup.getFilepath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File sao lưu không tồn tại trên Server.");
        }

        byte[] rawBytes;
        try {
            rawBytes = Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc tệp tin từ Server: " + e.getMessage());
        }

        restoreFromBytes(rawBytes, currentUser.getBranch().getId(), currentUser.getId(), currentUser);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. XÓA file backup trên server
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void deleteBackup(Integer backupId, User currentUser) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Nhân viên không có quyền thực hiện thao tác này.");
        }
        if (currentUser.getBranch() == null) {
            throw new RuntimeException("Tài khoản chưa được phân công vào chi nhánh nào.");
        }

        Backup backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản sao lưu với ID: " + backupId));

        if (!backup.getBranch().getId().equals(currentUser.getBranch().getId())) {
            throw new RuntimeException("Không có quyền xóa bản sao lưu của chi nhánh khác.");
        }

        try {
            Files.deleteIfExists(Paths.get(backup.getFilepath()));
        } catch (IOException e) {
            System.err.println("[BackupService] Không thể xóa file vật lý: " + backup.getFilepath());
        }

        backupRepository.delete(backup);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. AUTO BACKUP — Chạy lúc 1:00 AM hằng ngày
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Scheduled(cron = "0 0 1 * * ?")
    public void performScheduledBackup() {
        System.out.println("[BackupService] Bắt đầu chạy auto backup lúc " + LocalDateTime.now());
        List<Branch> branches = branchRepository.findAll();

        for (Branch branch : branches) {
            try {
                byte[] plainJson  = generateBackupJson(branch.getId());
                byte[] encrypted  = encryptData(plainJson);

                String dateStr   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String filename  = "auto_backup_branch_" + branch.getId() + "_" + dateStr + ".wbk";
                Path   branchDir = Paths.get(BACKUP_DIR + AUTO_BACKUP_SUBDIR + branch.getId());
                Files.createDirectories(branchDir);
                Path filePath = branchDir.resolve(filename);
                Files.write(filePath, encrypted);

                Backup backup = new Backup();
                backup.setBranch(branch);
                backup.setFilename(filename);
                backup.setFilepath(filePath.toString());
                backup.setFileSize((long) encrypted.length);
                backup.setBackupType("AUTO");
                backupRepository.save(backup);

                cleanupOldAutoBackups(branch.getId());

            } catch (Exception e) {
                System.err.println("[BackupService] Auto backup thất bại tại chi nhánh "
                        + branch.getId() + ": " + e.getMessage());
            }
        }
        System.out.println("[BackupService] Kết thúc auto backup lúc " + LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Dọn dẹp auto backup cũ hơn 14 ngày
    // ─────────────────────────────────────────────────────────────────────────
    private void cleanupOldAutoBackups(Integer branchId) {
        List<Backup> autoBackups = backupRepository.findByBranchIdAndBackupTypeOrderByCreatedAtAsc(branchId, "AUTO");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(14);

        for (Backup backup : autoBackups) {
            if (backup.getCreatedAt() != null && backup.getCreatedAt().isBefore(cutoff)) {
                try {
                    Files.deleteIfExists(Paths.get(backup.getFilepath()));
                } catch (IOException e) {
                    System.err.println("[BackupService] Không thể xóa file hết hạn: " + backup.getFilepath());
                }
                backupRepository.delete(backup);
                System.out.println("[BackupService] Đã dọn dẹp auto backup hết hạn: " + backup.getFilename());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Tạo JSON backup với chữ ký HMAC-SHA256
    //    Streaming API: dùng JsonGenerator ghi trực tiếp vào OutputStream
    //    thay vì load toàn bộ dữ liệu vào Map<String,Object> → tiết kiệm RAM
    //    Không có @Transactional vì được gọi từ exportBranchData (đã có TX)
    //    và performScheduledBackup (không cần TX đọc thuần)
    // ─────────────────────────────────────────────────────────────────────────
    private byte[] generateBackupJson(Integer branchId) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(STREAM_BUFFER_SIZE);

            // ── Bước 1: Streaming JSON bằng JsonGenerator ──────────────────
            try (JsonGenerator gen = objectMapper.getFactory().createGenerator(baos)) {
                gen.writeStartObject();

                // 1. Metadata (với signature=null — sẽ ký sau)
                Branch branch = branchRepository.findById(branchId).orElse(null);
                gen.writeObjectFieldStart("metadata");
                gen.writeNumberField("branchId", branchId);
                gen.writeStringField("branchName", branch != null ? branch.getName() : "Không tên");
                gen.writeStringField("backupAt", LocalDateTime.now().toString());
                gen.writeStringField("systemVersion", "2.0");
                gen.writeNullField("signature");
                gen.writeEndObject();

                // 2. Stream từng bảng — đọc từ DB và ghi thẳng vào output
                writeTableToGenerator(gen, "users",
                        "SELECT * FROM users WHERE branch_id = ?", branchId);
                writeTableToGenerator(gen, "customers",
                        "SELECT * FROM customers WHERE branch_id = ?", branchId);
                writeTableToGenerator(gen, "inventories",
                        "SELECT * FROM inventories WHERE branch_id = ?", branchId);
                writeTableToGenerator(gen, "receipts",
                        "SELECT * FROM receipts WHERE source_branch_id = ? OR dest_branch_id = ?",
                        branchId, branchId);
                writeTableToGenerator(gen, "receiptDetails",
                        "SELECT rd.* FROM receipt_details rd"
                        + " JOIN receipts r ON rd.receipt_id = r.id"
                        + " WHERE r.source_branch_id = ? OR r.dest_branch_id = ?",
                        branchId, branchId);
                writeTableToGenerator(gen, "stocktakes",
                        "SELECT * FROM stocktakes WHERE branch_id = ?", branchId);
                writeTableToGenerator(gen, "stocktakeDetails",
                        "SELECT sd.* FROM stocktake_details sd"
                        + " JOIN stocktakes s ON sd.stocktake_id = s.id"
                        + " WHERE s.branch_id = ?", branchId);

                gen.writeEndObject();
            }

            // ── Bước 2: Parse JSON → ký HMAC trên cùng format mà verify sẽ dùng ──
            byte[] jsonWithNullSig = baos.toByteArray();

            // Parse thành Map, serialize bằng objectMapper (cùng format với verify)
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(jsonWithNullSig, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");

            // Ký HMAC trên JSON có signature=null (serialize bằng objectMapper)
            String jsonForHmac = objectMapper.writeValueAsString(data);
            String signature = calculateHmac(jsonForHmac, secretKey);
            metadata.put("signature", signature);

            return objectMapper.writeValueAsBytes(data);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo file sao lưu: " + e.getMessage(), e);
        }
    }

    /**
     * Streaming helper: Ghi kết quả SQL query trực tiếp vào JsonGenerator
     * thay vì load toàn bộ vào List rồi mới serialize.
     * Mỗi row từ ResultSet được ghi ngay vào OutputStream → RAM usage = O(1 row).
     */
    private void writeTableToGenerator(JsonGenerator gen, String fieldName, String sql, Object... params) throws IOException {
        gen.writeArrayFieldStart(fieldName);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);
        for (Map<String, Object> row : rows) {
            gen.writeObject(row);
        }
        gen.writeEndArray();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Entry point restore — nhận raw bytes từ file (encrypted hoặc plain)
    //          Giải mã (nếu có) → chuyển sang JSON string → restoreFromJson
    // ─────────────────────────────────────────────────────────────────────────
    private void restoreFromBytes(byte[] rawBytes, Integer branchId, Integer currentUserId, User currentUser) {
        byte[] jsonBytes;
        try {
            jsonBytes = decryptData(rawBytes); // Tự detect: encrypted hay plain JSON
        } catch (Exception e) {
            throw new RuntimeException("Không thể giải mã file sao lưu: " + e.getMessage(), e);
        }
        String jsonContent = new String(jsonBytes, StandardCharsets.UTF_8);
        restoreFromJson(jsonContent, branchId, currentUserId, currentUser);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Điều phối toàn bộ quá trình restore từ JSON string
    //    Fix 3: Dùng BranchLockHelper (REQUIRES_NEW) + TransactionTemplate
    //           → Lock commit ngay lập tức (BranchLockInterceptor thấy ngay)
    //           → DELETE + INSERT trong 1 transaction: fail → rollback toàn bộ
    //           → Unlock trong finally: luôn chạy dù có exception
    //    Fix 6: Ghi audit log trước và sau
    // ─────────────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void restoreFromJson(String jsonContent, Integer branchId, Integer currentUserId, User currentUser) {
        // Bước 1: Khóa chi nhánh — commit ngay lập tức qua REQUIRES_NEW transaction
        branchLockHelper.lock(branchId);

        // Fix 6: Log trước khi bắt đầu restore
        try {
            auditLogService.logAction(currentUser, "RESTORE", "backups", String.valueOf(branchId),
                    "Bắt đầu phục hồi dữ liệu chi nhánh ID=" + branchId + " — Chi nhánh đã bị khóa.");
        } catch (Exception logEx) {
            System.err.println("[BackupService] Không thể ghi audit log: " + logEx.getMessage());
        }

        try {
            // Bước 2: Parse JSON và xác thực
            Map<String, Object> data = objectMapper.readValue(jsonContent, Map.class);
            Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
            if (metadata == null) {
                throw new RuntimeException("File sao lưu thiếu thông tin metadata.");
            }

            Integer fileBranchId = (Integer) metadata.get("branchId");
            if (!branchId.equals(fileBranchId)) {
                throw new RuntimeException("Dữ liệu sao lưu thuộc chi nhánh khác (branchId="
                        + fileBranchId + "), không được khôi phục tại chi nhánh này (branchId=" + branchId + ").");
            }

            // Fix 2: Xác minh HMAC-SHA256
            String providedSignature = (String) metadata.get("signature");
            if (providedSignature == null || providedSignature.isBlank()) {
                throw new RuntimeException("File sao lưu thiếu chữ ký bảo mật (signature). Tệp tin có thể bị giả mạo.");
            }
            metadata.put("signature", null);
            String jsonWithoutSignature = objectMapper.writeValueAsString(data);
            String expectedSignature    = calculateHmac(jsonWithoutSignature, secretKey);
            if (!expectedSignature.equals(providedSignature)) {
                throw new RuntimeException("Chữ ký HMAC-SHA256 không khớp — dữ liệu sao lưu đã bị thay đổi hoặc file không hợp lệ.");
            }

            // Bước 3: Thực thi DELETE + INSERT trong 1 transaction
            transactionTemplate.execute(status -> {
                try {
                    executeRestoreTransaction(data, branchId, currentUserId, currentUser);
                } catch (Exception e) {
                    status.setRollbackOnly();
                    throw new RuntimeException(e.getMessage(), e);
                }
                return null;
            });

            // Fix 6: Log sau khi restore thành công
            auditLogService.logAction(currentUser, "RESTORE", "backups", String.valueOf(branchId),
                    "Phục hồi dữ liệu chi nhánh ID=" + branchId + " thành công.");

        } catch (Exception e) {
            try {
                auditLogService.logAction(currentUser, "RESTORE", "backups", String.valueOf(branchId),
                        "Phục hồi dữ liệu chi nhánh ID=" + branchId + " THẤT BẠI: " + e.getMessage());
            } catch (Exception logEx) {
                System.err.println("[BackupService] Không thể ghi audit log lỗi: " + logEx.getMessage());
            }
            throw new RuntimeException(e.getMessage(), e);

        } finally {
            // Bước 4: Luôn mở khóa chi nhánh
            branchLockHelper.unlock(branchId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Thực hiện DELETE cũ + INSERT mới — chạy trong TransactionTemplate
    // ─────────────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void executeRestoreTransaction(Map<String, Object> data, Integer branchId, Integer currentUserId, User currentUser) {
        // 1. Xóa dữ liệu cũ theo đúng thứ tự ràng buộc khóa ngoại
        jdbcTemplate.update("DELETE FROM stocktake_details WHERE stocktake_id IN (SELECT id FROM stocktakes WHERE branch_id = ?)", branchId);
        jdbcTemplate.update("DELETE FROM stocktakes WHERE branch_id = ?", branchId);
        jdbcTemplate.update("DELETE FROM receipt_details WHERE receipt_id IN (SELECT id FROM receipts WHERE source_branch_id = ? OR dest_branch_id = ?)", branchId, branchId);
        jdbcTemplate.update("DELETE FROM receipts WHERE source_branch_id = ? OR dest_branch_id = ?", branchId, branchId);
        jdbcTemplate.update("DELETE FROM inventories WHERE branch_id = ?", branchId);
        jdbcTemplate.update("DELETE FROM customers WHERE branch_id = ?", branchId);
        // Xóa nhân viên chi nhánh, giữ lại tài khoản đang restore
        jdbcTemplate.update("DELETE FROM users WHERE branch_id = ? AND id != ?", branchId, currentUserId);

        // 2. Chèn dữ liệu mới từ file backup
        // Lọc bỏ user đang thực hiện restore để tránh duplicate (theo cả ID và username)
        List<Map<String, Object>> backupUsers   = (List<Map<String, Object>>) data.get("users");
        List<Map<String, Object>> usersToInsert = new ArrayList<>();
        if (backupUsers != null) {
            for (Map<String, Object> u : backupUsers) {
                Object idVal = u.get("id");
                Object usernameVal = u.get("username");
                if (idVal != null && ((Number) idVal).intValue() == currentUserId) {
                    continue; // Bỏ qua user đang restore (theo ID)
                }
                if (usernameVal != null && currentUser != null && usernameVal.toString().equalsIgnoreCase(currentUser.getUsername())) {
                    continue; // Bỏ qua user đang restore (theo username)
                }
                usersToInsert.add(u);
            }
        }
        insertTableData("users",            usersToInsert);
        insertTableData("customers",        (List<Map<String, Object>>) data.get("customers"));
        insertTableData("inventories",      (List<Map<String, Object>>) data.get("inventories"));
        insertTableData("receipts",         (List<Map<String, Object>>) data.get("receipts"));
        insertTableData("receipt_details",  (List<Map<String, Object>>) data.get("receiptDetails"));
        insertTableData("stocktakes",       (List<Map<String, Object>>) data.get("stocktakes"));
        insertTableData("stocktake_details",(List<Map<String, Object>>) data.get("stocktakeDetails"));

        // 3. Đồng bộ lại SERIAL sequences (Fix 4: xử lý đúng khi bảng rỗng)
        syncSequences();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: INSERT dữ liệu vào bảng với explicit ID
    // ─────────────────────────────────────────────────────────────────────────
    private void insertTableData(String tableName, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return;
        for (Map<String, Object> row : rows) {
            StringBuilder columns      = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            List<Object>  values       = new ArrayList<>();

            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (columns.length() > 0) {
                    columns.append(", ");
                    placeholders.append(", ");
                }
                columns.append(entry.getKey());
                placeholders.append("?");
                values.add(handlePostgresTypes(tableName, entry.getKey(), entry.getValue()));
            }
            String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                    tableName, columns, placeholders);
            jdbcTemplate.update(sql, values.toArray());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Ánh xạ kiểu dữ liệu Java → PostgreSQL (timestamps, dates, enums)
    // ─────────────────────────────────────────────────────────────────────────
    private Object handlePostgresTypes(String tableName, String columnName, Object value) {
        if (!(value instanceof String)) return value;
        String strVal = (String) value;

        // Timestamp columns
        if (columnName.endsWith("_at") || columnName.endsWith("_time")
                || "last_updated".equalsIgnoreCase(columnName)
                || "ban_until".equalsIgnoreCase(columnName)) {
            try { return java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(strVal)); } catch (Exception ignored) {}
            try { return java.sql.Timestamp.valueOf(java.time.OffsetDateTime.parse(strVal).toLocalDateTime()); } catch (Exception ignored) {}
            try { return java.sql.Timestamp.valueOf(strVal.replace("T", " ")); } catch (Exception ignored) {}
            System.err.println("[BackupService] Không parse được timestamp cho cột " + columnName + ": " + strVal);
            return value;
        }

        // Date columns
        if (columnName.endsWith("_date")) {
            try { return java.sql.Date.valueOf(strVal); } catch (Exception e) {
                System.err.println("[BackupService] Không parse được date cho cột " + columnName + ": " + strVal);
            }
            return value;
        }

        // PostgreSQL ENUM columns
        try {
            String pgType = resolvePgEnumType(tableName, columnName);
            if (pgType != null) {
                PGobject pgObj = new PGobject();
                pgObj.setType(pgType);
                pgObj.setValue(strVal);
                return pgObj;
            }
        } catch (Exception e) {
            System.err.println("[BackupService] Không thể tạo PGobject cho " + tableName + "." + columnName);
        }

        return value;
    }

    /** Tra cứu tên kiểu ENUM PostgreSQL theo (tableName, columnName). */
    private String resolvePgEnumType(String tableName, String columnName) {
        if ("receipts".equalsIgnoreCase(tableName) && "type".equalsIgnoreCase(columnName))   return "receipt_type";
        if ("receipts".equalsIgnoreCase(tableName) && "status".equalsIgnoreCase(columnName)) return "receipt_status";
        if ("stocktakes".equalsIgnoreCase(tableName) && "status".equalsIgnoreCase(columnName)) return "stocktake_status";
        if ("users".equalsIgnoreCase(tableName) && "role".equalsIgnoreCase(columnName))   return "user_role";
        if ("users".equalsIgnoreCase(tableName) && "status".equalsIgnoreCase(columnName)) return "user_status";
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Đồng bộ lại SERIAL sequence của PostgreSQL sau khi INSERT với explicit ID
    //    Fix 4: Không đặt sequence về 1 nếu bảng rỗng.
    //           Dùng GREATEST(max(id), 1) chỉ khi bảng có dữ liệu.
    //           Nếu bảng rỗng → giữ nguyên giá trị sequence hiện tại.
    // ─────────────────────────────────────────────────────────────────────────
    private void syncSequences() {
        String[] tables = {
            "users", "customers", "inventories",
            "receipts", "receipt_details",
            "stocktakes", "stocktake_details"
        };
        for (String table : tables) {
            try {
                // Chỉ cập nhật sequence khi bảng có ít nhất 1 dòng
                // GREATEST đảm bảo sequence không thụt lùi dưới 1
                jdbcTemplate.execute(String.format(
                    "DO $$ "
                    + "DECLARE seq_name TEXT; max_id BIGINT; "
                    + "BEGIN "
                    + "  seq_name := pg_get_serial_sequence('%s', 'id'); "
                    + "  SELECT MAX(id) INTO max_id FROM %s; "
                    + "  IF max_id IS NOT NULL THEN "
                    + "    PERFORM setval(seq_name, GREATEST(max_id, 1)); "
                    + "  END IF; "
                    + "END $$;",
                    table, table));
            } catch (Exception e) {
                System.err.println("[BackupService] Không thể sync sequence cho bảng " + table + ": " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Tính HMAC-SHA256 → Base64
    // ─────────────────────────────────────────────────────────────────────────
    private String calculateHmac(String data, String key) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKeySpec);
            byte[] hash = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tính HMAC-SHA256: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Nén GZIP
    // ─────────────────────────────────────────────────────────────────────────
    private byte[] compressGzip(byte[] data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length / 4);
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos, STREAM_BUFFER_SIZE)) {
                gzip.write(data);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi nén GZIP: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Giải nén GZIP
    // ─────────────────────────────────────────────────────────────────────────
    private byte[] decompressGzip(byte[] compressed) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(compressed.length * 4);
            try (GZIPInputStream gzip = new GZIPInputStream(bais, STREAM_BUFFER_SIZE)) {
                byte[] buffer = new byte[STREAM_BUFFER_SIZE];
                int len;
                while ((len = gzip.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi giải nén GZIP: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Mã hoá AES-256-GCM (v2: GZIP trước khi mã hoá)
    //
    // Pipeline: plaintext → GZIP compress → AES-256-GCM encrypt
    //
    // Định dạng output (binary):
    //   [MAGIC 4B] [VERSION 1B] [IV 12B] [Ciphertext + GCM AuthTag 16B]
    //
    // AES-GCM là AEAD (Authenticated Encryption with Associated Data):
    //   - Mã hoá đảm bảo bí mật nội dung
    //   - AuthTag tích hợp phát hiện giả mạo (tương đương HMAC ở tầng mã hoá)
    //   - IV ngẫu nhiên → cùng plaintext → ciphertext khác nhau mỗi lần
    //
    // Khoá AES: SHA-256(BACKUP_SECRET) → 32 bytes (256-bit)
    // ─────────────────────────────────────────────────────────────────────────
    private byte[] encryptData(byte[] plaintext) {
        try {
            // 0. Nén GZIP trước khi mã hoá (tiết kiệm 70-90% dung lượng)
            long originalSize = plaintext.length;
            byte[] compressed = compressGzip(plaintext);
            long compressedSize = compressed.length;
            double ratio = originalSize > 0 ? (1.0 - (double) compressedSize / originalSize) * 100 : 0;
            System.out.println("[BackupService] GZIP: " + formatBytes(originalSize)
                    + " → " + formatBytes(compressedSize)
                    + " (giảm " + String.format("%.1f", ratio) + "%)");

            // 1. Derive 256-bit AES key từ BACKUP_SECRET qua SHA-256
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] aesKeyBytes   = sha256.digest(secretKey.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            // 2. Tạo IV ngẫu nhiên 96-bit (khuyến nghị của NIST cho GCM)
            byte[] iv = new byte[GCM_IV_SIZE];
            new SecureRandom().nextBytes(iv);

            // 3. Mã hoá dữ liệu đã nén
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(compressed);

            // 4. Ghép output: MAGIC(4) + VERSION_V2(1) + IV(12) + Ciphertext
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(MAGIC_HEADER);       // 4 bytes
            out.write(ENC_VERSION_V2);     // 1 byte — v2 = GZIP + AES-GCM
            out.write(iv);                 // 12 bytes
            out.write(ciphertext);         // N + 16 bytes (data + auth tag)
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi mã hoá AES-256-GCM: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Giải mã AES-256-GCM — tương thích ngược đa phiên bản
    //
    // Logic phát hiện:
    //   - Bắt đầu bằng MAGIC "WHBK" + v2 → giải mã AES → giải nén GZIP
    //   - Bắt đầu bằng MAGIC "WHBK" + v1 → giải mã AES (không nén)
    //   - Bắt đầu bằng '{' (ASCII JSON)  → file cũ chưa mã hoá → trả nguyên
    //   - Trường hợp khác                → báo lỗi định dạng không hợp lệ
    // ─────────────────────────────────────────────────────────────────────────
    private byte[] decryptData(byte[] rawData) {
        if (rawData == null || rawData.length == 0) {
            throw new RuntimeException("File sao lưu rỗng hoặc không hợp lệ.");
        }

        // Phát hiện định dạng qua 4 byte đầu
        boolean isEncrypted = rawData.length >= MAGIC_HEADER.length
                && rawData[0] == MAGIC_HEADER[0]
                && rawData[1] == MAGIC_HEADER[1]
                && rawData[2] == MAGIC_HEADER[2]
                && rawData[3] == MAGIC_HEADER[3];

        // Tương thích ngược: file JSON thuần (bắt đầu bằng '{')
        if (!isEncrypted) {
            if (rawData[0] == '{') {
                System.out.println("[BackupService] Cảnh báo: Đang restore file backup cũ (chưa mã hoá). "
                        + "Khuyến nghị tạo bản sao lưu mới sau khi restore xong.");
                return rawData;
            }
            throw new RuntimeException("Định dạng file sao lưu không hợp lệ. Vui lòng sử dụng file .wbk do hệ thống tạo ra.");
        }

        // Minimum size: MAGIC(4) + VERSION(1) + IV(12) + AuthTag(16) = 33 bytes
        if (rawData.length < MAGIC_HEADER.length + 1 + GCM_IV_SIZE + (GCM_TAG_BITS / 8)) {
            throw new RuntimeException("File sao lưu bị hỏng hoặc không đủ dữ liệu.");
        }

        try {
            int offset = MAGIC_HEADER.length; // bỏ qua MAGIC
            byte version = rawData[offset];   // Đọc VERSION để xác định pipeline
            offset += 1;

            byte[] iv         = Arrays.copyOfRange(rawData, offset, offset + GCM_IV_SIZE);
            byte[] ciphertext = Arrays.copyOfRange(rawData, offset + GCM_IV_SIZE, rawData.length);

            // Derive 256-bit AES key
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] aesKeyBytes   = sha256.digest(secretKey.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            // Giải mã — AES-GCM tự xác thực AuthTag, ném AEADBadTagException nếu sai
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] decrypted = cipher.doFinal(ciphertext);

            // v2: GZIP compressed → giải nén sau khi giải mã
            if (version == ENC_VERSION_V2) {
                System.out.println("[BackupService] Phát hiện file v2 (GZIP + AES-GCM), đang giải nén...");
                decrypted = decompressGzip(decrypted);
            } else {
                System.out.println("[BackupService] Phát hiện file v1 (AES-GCM thuần, không nén).");
            }

            return decrypted;

        } catch (javax.crypto.AEADBadTagException e) {
            throw new RuntimeException("Xác thực GCM thất bại — file sao lưu đã bị giả mạo hoặc sai khoá giải mã.", e);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi giải mã AES-256-GCM: " + e.getMessage(), e);
        }
    }

    /** Định dạng kích thước file cho audit log (VD: "125.3 KB"). */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    // =========================================================================
    // ADMIN: SAO LƯU & PHỤC HỒI CẤU HÌNH HỆ THỐNG
    //   Phạm vi: branches, categories, products, users (ADMIN role)
    //   Không dùng branch lock — đây là dữ liệu toàn hệ thống, không giao dịch
    // =========================================================================

    private static final String SYSTEM_BACKUP_SUBDIR = "system/";

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Xuất file System Config Backup (.wbk mã hoá)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public byte[] exportSystemConfig(User currentUser) {
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Chỉ ADMIN mới có quyền sao lưu cấu hình hệ thống.");
        }

        byte[] plainJson = generateSystemConfigJson();
        byte[] encrypted = encryptData(plainJson);

        try {
            String dateStr    = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename   = "system_config_" + dateStr + ".wbk";
            Path   systemDir  = Paths.get(BACKUP_DIR + SYSTEM_BACKUP_SUBDIR);
            Files.createDirectories(systemDir);
            Path filePath = systemDir.resolve(filename);
            Files.write(filePath, encrypted);

            Backup record = new Backup();
            record.setBranch(null); // null = system-level, không thuộc chi nhánh
            record.setFilename(filename);
            record.setFilepath(filePath.toString());
            record.setFileSize((long) encrypted.length);
            record.setBackupType("MANUAL");
            record.setCreatedBy(currentUser);
            backupRepository.save(record);

            auditLogService.logAction(currentUser, "BACKUP", "system_config", "0",
                    "Admin đã tạo bản sao lưu cấu hình hệ thống: " + filename
                    + " (" + formatBytes(encrypted.length) + ")");

        } catch (Exception e) {
            throw new RuntimeException("Lỗi lưu file system backup: " + e.getMessage(), e);
        }

        return encrypted;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Phục hồi System Config từ file upload
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void importSystemConfig(MultipartFile file, User currentUser) {
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Chỉ ADMIN mới có quyền phục hồi cấu hình hệ thống.");
        }
        byte[] rawBytes;
        try {
            rawBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc tệp tin: " + e.getMessage());
        }
        restoreSystemFromBytes(rawBytes, currentUser);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Lịch sử System Config Backup
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<Backup> getSystemBackupHistory(User currentUser) {
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Chỉ ADMIN mới có quyền xem lịch sử sao lưu hệ thống.");
        }
        return backupRepository.findByBranchIsNullOrderByCreatedAtDesc();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Phục hồi System Config từ bản lưu trên server
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void restoreSystemFromHistory(Integer backupId, User currentUser) {
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Chỉ ADMIN mới có quyền phục hồi cấu hình hệ thống.");
        }

        Backup backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản sao lưu ID: " + backupId));

        if (backup.getBranch() != null) {
            throw new RuntimeException("Bản sao lưu này là dữ liệu chi nhánh, không phải cấu hình hệ thống.");
        }

        Path filePath = Paths.get(backup.getFilepath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File sao lưu không tồn tại trên Server.");
        }

        byte[] rawBytes;
        try {
            rawBytes = Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc tệp tin từ Server: " + e.getMessage());
        }

        restoreSystemFromBytes(rawBytes, currentUser);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Xóa file System Config Backup
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void deleteSystemBackup(Integer backupId, User currentUser) {
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Chỉ ADMIN mới có quyền xóa bản sao lưu hệ thống.");
        }

        Backup backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản sao lưu ID: " + backupId));

        if (backup.getBranch() != null) {
            throw new RuntimeException("Đây là backup chi nhánh, không phải system backup.");
        }

        try {
            Files.deleteIfExists(Paths.get(backup.getFilepath()));
        } catch (IOException e) {
            System.err.println("[BackupService] Không thể xóa file: " + backup.getFilepath());
        }

        auditLogService.logAction(currentUser, "DELETE", "system_config", String.valueOf(backupId),
                "Admin đã xóa bản sao lưu hệ thống: " + backup.getFilename());

        backupRepository.delete(backup);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Tạo JSON cấu hình hệ thống (không bao gồm dữ liệu giao dịch)
    // ─────────────────────────────────────────────────────────────────────────
    private byte[] generateSystemConfigJson() {
        try {
            Map<String, Object> data = new LinkedHashMap<>();

            // Metadata
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("backupScope", "SYSTEM_CONFIG");
            metadata.put("backupAt", LocalDateTime.now().toString());
            metadata.put("systemVersion", "1.0");
            metadata.put("signature", null);
            data.put("metadata", metadata);

            // 4 bảng cấu hình toàn hệ thống
            data.put("branches",   jdbcTemplate.queryForList("SELECT * FROM branches ORDER BY id"));
            data.put("categories", jdbcTemplate.queryForList("SELECT * FROM categories ORDER BY id"));
            data.put("products",   jdbcTemplate.queryForList("SELECT * FROM products ORDER BY id"));
            // Lưu tất cả tài khoản (ADMIN, MANAGER, STAFF) để đảm bảo dữ liệu phục hồi đầy đủ không bị lỗi khóa ngoại khi liên kết với chi nhánh
            data.put("adminUsers", jdbcTemplate.queryForList(
                    "SELECT * FROM users ORDER BY id"));

            // Ký HMAC-SHA256
            String jsonWithoutSig = objectMapper.writeValueAsString(data);
            String signature      = calculateHmac(jsonWithoutSig, secretKey);
            metadata.put("signature", signature);

            return objectMapper.writeValueAsBytes(data);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo file system config backup: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Thực hiện restore system config từ raw bytes
    // ─────────────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void restoreSystemFromBytes(byte[] rawBytes, User currentUser) {
        // Giải mã
        byte[] jsonBytes;
        try {
            jsonBytes = decryptData(rawBytes);
        } catch (Exception e) {
            throw new RuntimeException("Không thể giải mã file: " + e.getMessage(), e);
        }

        String jsonContent = new String(jsonBytes, StandardCharsets.UTF_8);

        auditLogService.logAction(currentUser, "RESTORE", "system_config", "0",
                "Bắt đầu phục hồi cấu hình hệ thống.");

        try {
            Map<String, Object> data     = objectMapper.readValue(jsonContent, Map.class);
            Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");

            if (metadata == null) {
                throw new RuntimeException("File thiếu metadata.");
            }

            // Kiểm tra đây phải là system config backup, không phải branch backup
            String scope = (String) metadata.get("backupScope");
            if (!"SYSTEM_CONFIG".equals(scope)) {
                throw new RuntimeException(
                    "Đây là file backup chi nhánh (branchId=" + metadata.get("branchId") + "), "
                    + "không phải System Config. Vui lòng dùng chức năng Phục hồi Chi nhánh.");
            }

            // Xác thực HMAC
            String providedSig = (String) metadata.get("signature");
            if (providedSig == null || providedSig.isBlank()) {
                throw new RuntimeException("File thiếu chữ ký bảo mật — có thể đã bị giả mạo.");
            }
            metadata.put("signature", null);
            String expectedSig = calculateHmac(objectMapper.writeValueAsString(data), secretKey);
            if (!expectedSig.equals(providedSig)) {
                throw new RuntimeException("Chữ ký HMAC không khớp — dữ liệu đã bị chỉnh sửa.");
            }

            // Restore trong Transaction: cập nhật branches/categories/products, thêm mới admin user nếu thiếu
            final Map<String, Object> finalData = data;
            transactionTemplate.execute(status -> {
                try {
                    executeSystemRestoreTransaction(finalData, currentUser.getId());
                } catch (Exception e) {
                    status.setRollbackOnly();
                    throw new RuntimeException(e.getMessage(), e);
                }
                return null;
            });

            auditLogService.logAction(currentUser, "RESTORE", "system_config", "0",
                    "Phục hồi cấu hình hệ thống thành công.");

        } catch (Exception e) {
            try {
                auditLogService.logAction(currentUser, "RESTORE", "system_config", "0",
                        "Phục hồi cấu hình hệ thống THẤT BẠI: " + e.getMessage());
            } catch (Exception ignore) {}
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Thực hiện restore system config trong transaction
    //   Chiến lược: UPSERT (không xóa toàn bộ) để tránh phá hủy dữ liệu giao dịch
    //   - branches/categories: UPDATE nếu tồn tại, INSERT nếu chưa có
    //   - products: UPSERT (ON CONFLICT DO UPDATE)
    //   - adminUsers: chỉ INSERT admin user chưa tồn tại (không đè password)
    // ─────────────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void executeSystemRestoreTransaction(Map<String, Object> data, Integer currentUserId) {
        List<Map<String, Object>> branches   = (List<Map<String, Object>>) data.get("branches");
        List<Map<String, Object>> categories = (List<Map<String, Object>>) data.get("categories");
        List<Map<String, Object>> products   = (List<Map<String, Object>>) data.get("products");
        List<Map<String, Object>> adminUsers = (List<Map<String, Object>>) data.get("adminUsers");

        // Branches: UPDATE tên/địa chỉ nếu đã có, INSERT nếu chưa có (không xóa chi nhánh đang chạy)
        if (branches != null) {
            for (Map<String, Object> row : branches) {
                jdbcTemplate.update(
                    "INSERT INTO branches (id, name, address, low_stock_threshold, is_locked) "
                    + "VALUES (?, ?, ?, ?, ?) "
                    + "ON CONFLICT (id) DO UPDATE SET name=EXCLUDED.name, address=EXCLUDED.address, "
                    + "low_stock_threshold=EXCLUDED.low_stock_threshold",
                    row.get("id"), row.get("name"), row.get("address"),
                    row.get("low_stock_threshold"), false);
            }
        }

        // Categories: UPSERT
        if (categories != null) {
            for (Map<String, Object> row : categories) {
                jdbcTemplate.update(
                    "INSERT INTO categories (id, name) VALUES (?, ?) "
                    + "ON CONFLICT (id) DO UPDATE SET name=EXCLUDED.name",
                    row.get("id"), row.get("name"));
            }
        }

        // Products: UPSERT toàn bộ thông tin sản phẩm (giá, code, v.v.)
        if (products != null) {
            for (Map<String, Object> row : products) {
                Object id = row.get("id");
                Object code = row.get("code");
                Object name = row.get("name");
                Object description = row.get("description");
                Object unit = row.get("unit");
                Object importPrice = row.get("import_price");
                Object price = row.get("price");
                Object categoryId = row.get("category_id");
                Object hasExpiry = row.get("has_expiry");
                Object imageUrl = row.get("image_url");
                Object mfgDate = handlePostgresTypes("products", "mfg_date", row.get("mfg_date"));
                Object expDate = handlePostgresTypes("products", "exp_date", row.get("exp_date"));
                Object isDeleted = row.get("is_deleted");
                Object createdAt = handlePostgresTypes("products", "created_at", row.get("created_at"));

                jdbcTemplate.update(
                    "INSERT INTO products (id, code, name, description, unit, import_price, price, category_id, has_expiry, image_url, mfg_date, exp_date, is_deleted, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT (id) DO UPDATE SET code=EXCLUDED.code, name=EXCLUDED.name, description=EXCLUDED.description, "
                    + "unit=EXCLUDED.unit, import_price=EXCLUDED.import_price, price=EXCLUDED.price, "
                    + "category_id=EXCLUDED.category_id, has_expiry=EXCLUDED.has_expiry, image_url=EXCLUDED.image_url, "
                    + "mfg_date=EXCLUDED.mfg_date, exp_date=EXCLUDED.exp_date, is_deleted=EXCLUDED.is_deleted, created_at=EXCLUDED.created_at",
                    id, code, name, description, unit, importPrice, price, categoryId, hasExpiry, imageUrl, mfgDate, expDate, isDeleted, createdAt);
            }
        }

        // Users (admin & employees): chỉ INSERT nếu chưa tồn tại (không ghi đè mật khẩu hiện tại)
        if (adminUsers != null) {
            for (Map<String, Object> row : adminUsers) {
                Object idVal = row.get("id");
                if (idVal != null && ((Number) idVal).intValue() == currentUserId) continue;

                Object banUntil = handlePostgresTypes("users", "ban_until", row.get("ban_until"));
                Object createdAt = handlePostgresTypes("users", "created_at", row.get("created_at"));

                // ON CONFLICT DO NOTHING: nếu user đã tồn tại → giữ nguyên, không đè
                jdbcTemplate.update(
                    "INSERT INTO users (id, username, password, full_name, role, status, branch_id, ban_until, created_at) "
                    + "VALUES (?, ?, ?, ?, ?::user_role, ?::user_status, ?, ?, ?) "
                    + "ON CONFLICT (id) DO NOTHING",
                    row.get("id"), row.get("username"), row.get("password"),
                    row.get("full_name"), row.get("role"), row.get("status"), row.get("branch_id"), banUntil, createdAt);
            }
        }

        syncSequences();
    }

    // =========================================================================
    // DEMO/TEST: Xóa toàn bộ dữ liệu giao dịch chi nhánh (dùng để test backup)
    //   Giữ lại: user đang thao tác, thông tin chi nhánh
    //   Xoá sạch: tồn kho, phiếu kho, kiểm kê, khách hàng, nhân viên khác
    // =========================================================================
    @Override
    public void wipeBranchData(User currentUser) {
        if (currentUser.getRole() == UserRole.STAFF) {
            throw new RuntimeException("Nhân viên không có quyền thực hiện thao tác này.");
        }
        if (currentUser.getBranch() == null) {
            throw new RuntimeException("Tài khoản chưa được phân công vào chi nhánh nào.");
        }

        Integer branchId   = currentUser.getBranch().getId();
        Integer currentUserId = currentUser.getId();

        // Khóa chi nhánh trong quá trình xóa
        branchLockHelper.lock(branchId);

        try {
            auditLogService.logAction(currentUser, "WIPE", "branch_data", String.valueOf(branchId),
                    "Bắt đầu xóa toàn bộ dữ liệu chi nhánh ID=" + branchId + " (DEMO/TEST).");

            transactionTemplate.execute(status -> {
                try {
                    // Xóa theo đúng thứ tự ràng buộc khóa ngoại
                    int sdDel = jdbcTemplate.update("DELETE FROM stocktake_details WHERE stocktake_id IN (SELECT id FROM stocktakes WHERE branch_id = ?)", branchId);
                    int sDel  = jdbcTemplate.update("DELETE FROM stocktakes WHERE branch_id = ?", branchId);
                    int rdDel = jdbcTemplate.update("DELETE FROM receipt_details WHERE receipt_id IN (SELECT id FROM receipts WHERE source_branch_id = ? OR dest_branch_id = ?)", branchId, branchId);
                    int rDel  = jdbcTemplate.update("DELETE FROM receipts WHERE source_branch_id = ? OR dest_branch_id = ?", branchId, branchId);
                    int iDel  = jdbcTemplate.update("DELETE FROM inventories WHERE branch_id = ?", branchId);

                    // Gỡ FK tham chiếu chéo chi nhánh trước khi xóa customers/users
                    // (receipts từ chi nhánh khác có thể tham chiếu customer/user của chi nhánh này)
                    jdbcTemplate.update(
                        "UPDATE receipts SET customer_id = NULL WHERE customer_id IN (SELECT id FROM customers WHERE branch_id = ?)", branchId);
                    jdbcTemplate.update(
                        "UPDATE receipts SET created_by = (SELECT MIN(id) FROM users WHERE role = 'ADMIN') WHERE created_by IN (SELECT id FROM users WHERE branch_id = ? AND id != ?)", branchId, currentUserId);
                    jdbcTemplate.update(
                        "UPDATE receipts SET stocktake_by_id = NULL WHERE stocktake_by_id IN (SELECT id FROM users WHERE branch_id = ? AND id != ?)", branchId, currentUserId);

                    int cDel  = jdbcTemplate.update("DELETE FROM customers WHERE branch_id = ?", branchId);
                    int uDel  = jdbcTemplate.update("DELETE FROM users WHERE branch_id = ? AND id != ?", branchId, currentUserId);

                    System.out.println("[BackupService] WIPE chi nhánh " + branchId
                            + ": stocktake_details=" + sdDel + ", stocktakes=" + sDel
                            + ", receipt_details=" + rdDel + ", receipts=" + rDel
                            + ", inventories=" + iDel + ", customers=" + cDel
                            + ", users=" + uDel);

                    syncSequences();
                } catch (Exception e) {
                    status.setRollbackOnly();
                    throw new RuntimeException(e.getMessage(), e);
                }
                return null;
            });

            auditLogService.logAction(currentUser, "WIPE", "branch_data", String.valueOf(branchId),
                    "Xóa toàn bộ dữ liệu chi nhánh ID=" + branchId + " thành công (DEMO/TEST).");

        } catch (Exception e) {
            try {
                auditLogService.logAction(currentUser, "WIPE", "branch_data", String.valueOf(branchId),
                        "Xóa dữ liệu chi nhánh ID=" + branchId + " THẤT BẠI: " + e.getMessage());
            } catch (Exception ignore) {}
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            branchLockHelper.unlock(branchId);
        }
    }
}

