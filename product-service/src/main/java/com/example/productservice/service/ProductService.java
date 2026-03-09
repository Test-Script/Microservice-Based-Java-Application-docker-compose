package com.example.productservice.service;

import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getAvailableProducts() {
        return productRepository.findByAvailableTrue();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    public Product createProduct(Product product) {
        Product saved = productRepository.save(product);
        log.info("Created product: {}", saved.getId());
        return saved;
    }

    public Product updateProduct(Long id, Product updated) {
        Product existing = getProductById(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setPrice(updated.getPrice());
        existing.setStockQuantity(updated.getStockQuantity());
        existing.setAvailable(updated.isAvailable());
        return productRepository.save(existing);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
        log.info("Deleted product: {}", id);
    }

    /**
     * Called by Order Service to reserve stock when placing an order.
     */
    @Transactional
    public boolean reserveStock(Long productId, int quantity) {
        Product product = getProductById(productId);
        if (!product.isAvailable() || product.getStockQuantity() < quantity) {
            log.warn("Insufficient stock for product {}: requested={}, available={}",
                    productId, quantity, product.getStockQuantity());
            return false;
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
        if (product.getStockQuantity() == 0) {
            product.setAvailable(false);
        }
        productRepository.save(product);
        log.info("Reserved {} units of product {}", quantity, productId);
        return true;
    }

    /**
     * Called by Order Service to release stock when an order is cancelled.
     */
    @Transactional
    public void releaseStock(Long productId, int quantity) {
        Product product = getProductById(productId);
        product.setStockQuantity(product.getStockQuantity() + quantity);
        product.setAvailable(true);
        productRepository.save(product);
        log.info("Released {} units of product {}", quantity, productId);
    }
}
