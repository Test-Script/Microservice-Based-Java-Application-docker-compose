package com.example.productservice;

import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
@Slf4j
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner seedProducts(ProductRepository repo) {
        return args -> {
            repo.save(Product.builder()
                    .name("Laptop Pro 15")
                    .description("High-performance laptop for professionals")
                    .price(new BigDecimal("1299.99"))
                    .stockQuantity(50)
                    .category(Product.ProductCategory.ELECTRONICS)
                    .build());

            repo.save(Product.builder()
                    .name("Spring Boot in Action")
                    .description("Comprehensive guide to Spring Boot development")
                    .price(new BigDecimal("49.99"))
                    .stockQuantity(200)
                    .category(Product.ProductCategory.BOOKS)
                    .build());

            repo.save(Product.builder()
                    .name("Developer Hoodie")
                    .description("Comfortable hoodie for late-night coding sessions")
                    .price(new BigDecimal("59.99"))
                    .stockQuantity(100)
                    .category(Product.ProductCategory.CLOTHING)
                    .build());

            log.info("Seeded 3 products into the database.");
        };
    }
}
