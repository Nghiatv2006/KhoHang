package com.example.Hehe.service;

import com.example.Hehe.dto.InventoryResponse;
import com.example.Hehe.model.User;

import java.util.List;

public interface InventoryService {
    List<InventoryResponse> getInventories(Integer branchId, User currentUser);
    InventoryResponse addStock(Integer id, Integer quantityToAdd, User currentUser);
    InventoryResponse createInventory(com.example.Hehe.dto.InventorySaveRequest request, User currentUser);
    InventoryResponse createProductWithInventory(com.example.Hehe.dto.ProductWithInventoryRequest request, User currentUser);
    InventoryResponse updateExpiryWarning(Integer id, Integer expiryWarningDays, User currentUser);
    void deleteInventory(Integer id, User currentUser);
    List<InventoryResponse> getGlobalInventories();
}
