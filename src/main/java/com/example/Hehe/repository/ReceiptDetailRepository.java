package com.example.Hehe.repository;

import com.example.Hehe.model.ReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiptDetailRepository extends JpaRepository<ReceiptDetail, Integer> {
    List<ReceiptDetail> findByReceiptId(Integer receiptId);
}
