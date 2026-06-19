package com.example.Hehe.repository;

import com.example.Hehe.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {
    List<Inventory> findByBranchId(Integer branchId);
    List<Inventory> findByProductId(Integer productId);
    List<Inventory> findByBranchIdAndProductId(Integer branchId, Integer productId);
    java.util.Optional<Inventory> findByBranchIdAndProductIdAndBatchCode(Integer branchId, Integer productId, String batchCode);
    boolean existsByProductIdAndQuantityGreaterThan(Integer productId, int quantity);
}

