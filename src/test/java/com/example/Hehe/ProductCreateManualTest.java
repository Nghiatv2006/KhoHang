package com.example.Hehe;

import com.example.Hehe.model.Product;
import com.example.Hehe.model.Category;
import com.example.Hehe.repository.ProductRepository;
import com.example.Hehe.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProductCreateManualTest {
    @Autowired
    ProductRepository productRepository;
    @Autowired
    CategoryRepository categoryRepository;

    @Test
    void testCreateProduct() {
        System.out.println(">>> Bắt đầu test tạo sản phẩm...");
        Category category = categoryRepository.findAll().stream().findFirst().orElse(null);
        if (category == null) {
            category = new Category();
            category.setName("Test Category " + UUID.randomUUID().toString().substring(0,4));
            category = categoryRepository.save(category);
        }

        Product p = new Product();
        p.setSku("TEST-" + UUID.randomUUID().toString().substring(0, 4));
        p.setName("Sản phẩm Test Tự Động");
        p.setImportPrice(BigDecimal.valueOf(10000));
        p.setPrice(BigDecimal.valueOf(15000));
        p.setUnit("Cái");
        p.setHasExpiry(false);
        p.setCategory(category);
        
        Product saved = productRepository.save(p);
        System.out.println("========== SUCCESS! TẠO SẢN PHẨM THÀNH CÔNG VỚI ID: " + saved.getId() + " ==========");
    }
}
