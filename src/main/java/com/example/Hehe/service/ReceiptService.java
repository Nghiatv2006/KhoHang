package com.example.Hehe.service;

import com.example.Hehe.dto.ReceiptResponse;
import com.example.Hehe.dto.ReceiptSaveRequest;
import com.example.Hehe.model.User;

import java.util.List;

public interface ReceiptService {
    List<ReceiptResponse> getAllReceipts(User currentUser);
    ReceiptResponse getReceiptById(Integer id, User currentUser);
    ReceiptResponse createReceipt(ReceiptSaveRequest request, User currentUser);
    ReceiptResponse cancelReceipt(Integer id, User currentUser);
    ReceiptResponse approveReceipt(Integer id, User currentUser);
    ReceiptResponse markPaid(Integer id, User currentUser);
    ReceiptResponse confirmTransfer(Integer id, java.util.Map<String, Object> payload, User currentUser);
    ReceiptResponse confirmStocktake(Integer id, java.util.Map<String, Object> payload, User currentUser);
    List<ReceiptResponse> getReceiptsByCustomer(Integer customerId, User currentUser);
}
