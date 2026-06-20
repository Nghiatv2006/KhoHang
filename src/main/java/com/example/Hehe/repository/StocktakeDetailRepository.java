package com.example.Hehe.repository;

import com.example.Hehe.model.StocktakeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StocktakeDetailRepository extends JpaRepository<StocktakeDetail, Integer> {
}
