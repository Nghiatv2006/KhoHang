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
}
