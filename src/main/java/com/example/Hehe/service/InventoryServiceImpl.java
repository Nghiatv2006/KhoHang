package com.example.Hehe.service;

import com.example.Hehe.dto.InventoryResponse;
import com.example.Hehe.model.Inventory;
import com.example.Hehe.model.User;
import com.example.Hehe.model.UserRole;
import com.example.Hehe.repository.InventoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public List<InventoryResponse> getInventories(Integer branchId, User currentUser) {
        List<Inventory> inventories;

        if (currentUser.getRole() == UserRole.ADMIN) {
            if (branchId != null) {
                inventories = inventoryRepository.findByBranchId(branchId);
            } else {
                inventories = inventoryRepository.findAll();
            }
        } else {
            // MANAGER and STAFF can only view their own branch
            if (currentUser.getBranch() == null) {
                throw new RuntimeException("Bạn chưa được phân công vào chi nhánh nào.");
            }
            Integer myBranchId = currentUser.getBranch().getId();
            
            if (branchId != null && !branchId.equals(myBranchId)) {
                throw new RuntimeException("Bạn chỉ có quyền xem tồn kho của chi nhánh mình.");
            }
            inventories = inventoryRepository.findByBranchId(myBranchId);
        }

        return inventories.stream()
                .map(InventoryResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryResponse> getGlobalInventories() {
        return inventoryRepository.findAll().stream()
                .map(InventoryResponse::new)
                .collect(Collectors.toList());
    }
}
