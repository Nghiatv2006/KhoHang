package com.example.Hehe.controller;

import com.example.Hehe.dto.AuditLogResponse;
import com.example.Hehe.model.AuditLog;
import com.example.Hehe.model.User;
import com.example.Hehe.service.AuditLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller cung cấp API tra cứu Nhật ký hoạt động.
 * Chỉ ADMIN và MANAGER được phép truy cập.
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * API lấy danh sách Nhật ký hoạt động với bộ lọc nâng cao.
     * GET /api/audit-logs?userId=&action=&from=&to=&keyword=
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<org.springframework.data.domain.Page<AuditLogResponse>> getLogs(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<AuditLog> logs = auditLogService.searchLogs(currentUser, userId, action, from, to, keyword, pageable);
        org.springframework.data.domain.Page<AuditLogResponse> response = logs.map(AuditLogResponse::new);
        return ResponseEntity.ok(response);
    }
}
