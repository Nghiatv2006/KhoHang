package com.example.Hehe.repository;

import com.example.Hehe.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Integer> {
    List<Receipt> findBySourceBranchId(Integer branchId);
    List<Receipt> findByDestBranchId(Integer branchId);
    List<Receipt> findByCustomerId(Integer customerId);
}
