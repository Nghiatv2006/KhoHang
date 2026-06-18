package com.example.Hehe.repository;

import com.example.Hehe.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Integer> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Integer id);
    List<Branch> findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(String name, String address);

    List<Branch> findByIsHeadTrue();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Branch b SET b.isHead = false WHERE b.id <> :excludeId")
    void demoteOtherHeadBranches(@Param("excludeId") Integer excludeId);
}
