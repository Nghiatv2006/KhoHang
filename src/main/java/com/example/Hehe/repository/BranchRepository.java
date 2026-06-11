package com.example.Hehe.repository;

import com.example.Hehe.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Integer> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Integer id);
    List<Branch> findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(String name, String address);
}
