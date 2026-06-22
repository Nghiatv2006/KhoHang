package com.example.Hehe.repository;

import com.example.Hehe.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository thao tác với bảng products trong cơ sở dữ liệu.
 * Bổ sung JpaSpecificationExecutor để hỗ trợ tìm kiếm động (nhiều tiêu chí).
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    // Tìm kiếm danh sách sản phẩm theo mã danh mục
    List<Product> findByCategoryId(Integer categoryId);

    // Tìm kiếm sản phẩm theo mã SKU (do SKU là unique)
    Product findBySku(String sku);

    // Tìm kiếm sản phẩm đã bị xóa mềm theo tên
    java.util.Optional<Product> findFirstByNameAndIsDeletedTrue(String name);
}
