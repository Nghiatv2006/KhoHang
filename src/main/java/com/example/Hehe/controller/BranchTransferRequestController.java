package com.example.Hehe.controller;

import com.example.Hehe.dto.TransferRequestResponse;
import com.example.Hehe.dto.TransferRequestSaveRequest;
import com.example.Hehe.model.User;
import com.example.Hehe.service.BranchTransferRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/transfer-requests")
public class BranchTransferRequestController {

    private final BranchTransferRequestService requestService;

    public BranchTransferRequestController(BranchTransferRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    public ResponseEntity<?> createRequest(
            @RequestBody TransferRequestSaveRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            TransferRequestResponse response = requestService.createRequest(request, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getRequests(@AuthenticationPrincipal User currentUser) {
        try {
            List<TransferRequestResponse> responses = requestService.getRequests(currentUser);
            return ResponseEntity.ok(responses);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        try {
            TransferRequestResponse response = requestService.approveRequest(id, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        try {
            TransferRequestResponse response = requestService.rejectRequest(id, currentUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
