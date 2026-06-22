package com.example.Hehe.service;

import com.example.Hehe.dto.TransferRequestResponse;
import com.example.Hehe.dto.TransferRequestSaveRequest;
import com.example.Hehe.model.User;
import java.util.List;

public interface BranchTransferRequestService {
    TransferRequestResponse createRequest(TransferRequestSaveRequest request, User currentUser);
    
    List<TransferRequestResponse> getRequests(User currentUser);
    
    TransferRequestResponse approveRequest(Integer requestId, User currentUser);
    
    TransferRequestResponse rejectRequest(Integer requestId, User currentUser);
}
