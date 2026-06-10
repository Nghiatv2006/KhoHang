package com.example.Hehe.repository;

import com.example.Hehe.model.User;
import com.example.Hehe.model.UserRole;
import com.example.Hehe.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByEmailAndIdNot(String email, Integer id);

    @Query("SELECT u FROM User u WHERE " +
           "(:pattern IS NULL OR LOWER(u.username) LIKE :pattern " +
           "OR LOWER(u.fullName) LIKE :pattern " +
           "OR LOWER(u.email) LIKE :pattern) " +
           "AND u.role = COALESCE(:role, u.role) " +
           "AND (:branchId IS NULL OR u.branch.id = :branchId) " +
           "AND u.status = COALESCE(:status, u.status)")
    List<User> searchUsers(
        @Param("pattern") String pattern,
        @Param("role") UserRole role,
        @Param("branchId") Integer branchId,
        @Param("status") UserStatus status
    );
}
