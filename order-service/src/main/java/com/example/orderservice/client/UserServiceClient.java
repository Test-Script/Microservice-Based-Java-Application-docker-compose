package com.example.orderservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Component
@Slf4j
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient(@Value("${services.user-service.url}") String userServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    /**
     * Checks if the user exists and is in ACTIVE status.
     *
     * @param userId the user's ID
     * @return true if active, false otherwise
     */
    public boolean isUserActive(Long userId) {
        try {
            Map response = webClient.get()
                    .uri("/api/users/{id}/active", userId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && Boolean.TRUE.equals(response.get("active"))) {
                log.info("User {} is active", userId);
                return true;
            }
            return false;
        } catch (WebClientResponseException.NotFound e) {
            log.warn("User {} not found in User Service", userId);
            throw new RuntimeException("User not found: " + userId);
        } catch (Exception e) {
            log.error("Failed to reach User Service for userId={}: {}", userId, e.getMessage());
            throw new RuntimeException("User Service unavailable. Please try again later.");
        }
    }
}
