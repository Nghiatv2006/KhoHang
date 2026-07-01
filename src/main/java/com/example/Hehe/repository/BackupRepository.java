package com.example.Hehe.repository;

import com.example.Hehe.model.Backup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupRepository extends JpaRepository<Backup, Integer> {
    // Branch backup
    List<Backup> findByBranchIdOrderByCreatedAtDesc(Integer branchId);
    List<Backup> findByBranchIdAndBackupTypeOrderByCreatedAtAsc(Integer branchId, String backupType);

    // System config backup (branch = null)
    List<Backup> findByBranchIsNullOrderByCreatedAtDesc();
    List<Backup> findByBranchIsNullAndBackupTypeOrderByCreatedAtAsc(String backupType);
}
