package com.example.Hehe.repository;

import com.example.Hehe.model.Stocktake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StocktakeRepository extends JpaRepository<Stocktake, Integer> {
    List<Stocktake> findByBranchIdOrderByCreatedAtDesc(Integer branchId);
    List<Stocktake> findAllByOrderByCreatedAtDesc();
}
