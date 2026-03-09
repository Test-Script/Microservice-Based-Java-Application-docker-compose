package com.example.orderservice.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
public class ProductServiceClient {

    private final WebClient webClient;

    public ProductServiceClient(@Value("${services.product-service.url}") String productServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(productServiceUrl)
                .build();
    }

    /**
     * Fetches product details from the Product Service.
     */
    public ProductDto getProduct(Long productId) {
        try {
            return webClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(ProductDto.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("Product not found: " + productId);
        } catch (Exception e) {
            log.error("Failed to reach Product Service for productId={}: {}", productId, e.getMessage());
            throw new RuntimeException("Product Service unavailable. Please try again later.");
        }
    }

    /**
     * Asks Product Service to reserve stock for an order.
     *
     * @return true if stock was successfully reserved
     */
    public boolean reserveStock(Long productId, int quantity) {
        try {
            Map response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/products/{id}/reserve")
                            .queryParam("quantity", quantity)
                            .build(productId))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            boolean reserved = response != null && Boolean.TRUE.equals(response.get("reserved"));
            log.info("Stock reservation for product {} x{}: {}", productId, quantity, reserved);
            return reserved;
        } catch (Exception e) {
            log.error("Stock reservation failed for product {}: {}", productId, e.getMessage());
            throw new RuntimeException("Product Service unavailable. Please try again later.");
        }
    }

    /**
     * Asks Product Service to release stock when an order is cancelled.
     */
    public void releaseStock(Long productId, int quantity) {
        try {
            webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/products/{id}/release")
                            .queryParam("quantity", quantity)
                            .build(productId))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Released stock for product {} x{}", productId, quantity);
        } catch (Exception e) {
            log.error("Stock release failed for product {}: {}", productId, e.getMessage());
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDto {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer stockQuantity;
        private boolean available;
    }
}
