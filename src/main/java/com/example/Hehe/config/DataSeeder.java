package com.example.Hehe.config;

import com.example.Hehe.model.Product;
import com.example.Hehe.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(ProductRepository productRepository) {
        return args -> {
            List<Product> products = productRepository.findAll();
            for (Product p : products) {
                if (p.getImageUrl() == null || p.getImageUrl().isEmpty()) {
                    String imgUrl = "";
                    switch (p.getSku()) {
                        case "IP15": imgUrl = "http://localhost:8080/uploads/images/iphone_15.png"; break;
                        case "MACM2": imgUrl = "http://localhost:8080/uploads/images/macbook_air.png"; break;
                        case "AIRPODS": imgUrl = "http://localhost:8080/uploads/images/airpods_pro.png"; break;
                        case "MILK_OLD": imgUrl = "http://localhost:8080/uploads/images/milk_old.png"; break;
                        case "MILK_NEW": imgUrl = "http://localhost:8080/uploads/images/milk_new.png"; break;
                    }
                    if (!imgUrl.isEmpty()) {
                        p.setImageUrl(imgUrl);
                        productRepository.save(p);
                    }
                }
            }
        };
    }
}
