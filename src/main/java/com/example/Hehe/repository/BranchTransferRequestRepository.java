package com.example.Hehe.repository;

import com.example.Hehe.model.BranchTransferRequest;
import com.example.Hehe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchTransferRequestRepository extends JpaRepository<BranchTransferRequest, Integer> {
    List<BranchTransferRequest> findByStatus(String status);
    
    List<BranchTransferRequest> findByCreatedBy(User createdBy);
    
    List<BranchTransferRequest> findByCreatedByAndStatus(User createdBy, String status);
    
    boolean existsByStaffAndStatus(User staff, String status);
    
    void deleteByStaffId(Integer staffId);
}
