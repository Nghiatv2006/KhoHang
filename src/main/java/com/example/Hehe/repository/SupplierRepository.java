package com.example.Hehe.repository;

import com.example.Hehe.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    
    @Query("SELECT s FROM Supplier s WHERE " +
           "(:pattern IS NULL OR LOWER(s.name) LIKE :pattern " +
           "OR LOWER(s.contactInfo) LIKE :pattern " +
           "OR LOWER(s.address) LIKE :pattern) " +
           "AND s.status = COALESCE(:status, s.status)")
    List<Supplier> searchSuppliers(
        @Param("pattern") String pattern,
        @Param("status") String status
    );
}
