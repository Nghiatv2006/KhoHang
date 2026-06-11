package com.example.Hehe.repository;

import com.example.Hehe.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository xử lý các thao tác tương tác với bảng categories trong DB.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    /**
     * Kiểm tra xem tên danh mục đã tồn tại chưa
     * @param name Tên danh mục
     * @return true nếu tồn tại, ngược lại false
     */
    boolean existsByName(String name);
    
    /**
     * Tìm danh mục theo tên, sử dụng khi tìm kiếm
     * @param name Tên hoặc một phần của tên
     * @return Danh sách các danh mục khớp
     */
    List<Category> findByNameContainingIgnoreCase(String name);
}
