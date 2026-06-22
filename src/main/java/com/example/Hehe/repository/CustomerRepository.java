package com.example.Hehe.repository;

import com.example.Hehe.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    
    @Query("SELECT c FROM Customer c WHERE " +
           "(:pattern IS NULL OR LOWER(c.name) LIKE :pattern " +
           "OR LOWER(c.contactInfo) LIKE :pattern " +
           "OR LOWER(c.address) LIKE :pattern) " +
           "AND c.status = COALESCE(:status, c.status)")
    List<Customer> searchCustomers(
        @Param("pattern") String pattern,
        @Param("status") String status
    );
    java.util.Optional<Customer> findByName(String name);
}
