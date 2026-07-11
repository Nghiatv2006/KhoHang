package com.example.Hehe.service;

import com.example.Hehe.dto.EditReceiptRequest;
import com.example.Hehe.dto.ReceiptEditLogResponse;
import com.example.Hehe.dto.ReceiptResponse;
import com.example.Hehe.dto.ReceiptSaveRequest;
import com.example.Hehe.model.User;

import java.util.List;

public interface ReceiptService {
    List<ReceiptResponse> getAllReceipts(User currentUser);
    ReceiptResponse getReceiptById(Integer id, User currentUser);
    ReceiptResponse createReceipt(ReceiptSaveRequest request, User currentUser);
    ReceiptResponse cancelReceipt(Integer id, String reason, User currentUser);
    ReceiptResponse approveReceipt(Integer id, User currentUser);
    ReceiptResponse markPaid(Integer id, User currentUser);
    ReceiptResponse confirmTransfer(Integer id, java.util.Map<String, Object> payload, User currentUser);
    ReceiptResponse confirmStocktake(Integer id, java.util.Map<String, Object> payload, User currentUser);
    ReceiptResponse approveShortfall(Integer id, boolean isApproved, User currentUser);
    ReceiptResponse compensateShortfall(Integer id, User currentUser);
    List<ReceiptResponse> getReceiptsByCustomer(Integer customerId, User currentUser);
    List<ReceiptResponse> getCompletedBranchReceipts(User currentUser);

    /** Staff tự chỉnh sửa phiếu (chỉ khi DRAFT) */
    ReceiptResponse editReceiptByStaff(Integer id, EditReceiptRequest request, User currentUser);

    /** Manager chỉnh sửa phiếu:
     *  - DRAFT → ghi MANAGER_TO_STAFF log, đổi status thành PENDING_STAFF_CONFIRM
     *  - PENDING_ADMIN → ghi MANAGER_TO_ADMIN log, giữ nguyên status
     */
    ReceiptResponse editReceiptByManager(Integer id, EditReceiptRequest request, User currentUser);

    /** Staff xác nhận thay đổi của Manager (khi status = PENDING_STAFF_CONFIRM) */
    ReceiptResponse staffAcknowledgeEdit(Integer id, User currentUser);

    /** Lấy lịch sử chỉnh sửa phiếu (Admin chỉ thấy khi phiếu >= PENDING_ADMIN) */
    List<ReceiptEditLogResponse> getEditHistory(Integer id, User currentUser);
}

