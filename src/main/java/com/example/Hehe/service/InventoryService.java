package com.example.Hehe.service;

import com.example.Hehe.dto.InventoryResponse;
import com.example.Hehe.model.User;

import java.util.List;

public interface InventoryService {
    List<InventoryResponse> getInventories(Integer branchId, User currentUser);
    List<InventoryResponse> getGlobalInventories();
}
