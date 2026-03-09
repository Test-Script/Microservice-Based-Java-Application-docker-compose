package com.example.orderservice.service;

import com.example.orderservice.client.ProductServiceClient;
import com.example.orderservice.client.UserServiceClient;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    /**
     * Places a new order.
     * Workflow:
     *   1. Validate user is active  → calls User Service
     *   2. Fetch product details    → calls Product Service
     *   3. Reserve stock            → calls Product Service
     *   4. Persist order locally
     */
    @Transactional
    public Order placeOrder(Long userId, Long productId, int quantity, String notes) {
        log.info("Placing order: userId={}, productId={}, quantity={}", userId, productId, quantity);

        // Step 1: Validate user via User Service
        boolean userActive = userServiceClient.isUserActive(userId);
        if (!userActive) {
            throw new RuntimeException("Cannot place order: User " + userId + " is not active.");
        }

        // Step 2: Fetch product info via Product Service
        ProductServiceClient.ProductDto product = productServiceClient.getProduct(productId);
        if (!product.isAvailable()) {
            throw new RuntimeException("Product " + productId + " is currently unavailable.");
        }
        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock. Requested: " + quantity
                    + ", Available: " + product.getStockQuantity());
        }

        // Step 3: Reserve stock via Product Service
        boolean reserved = productServiceClient.reserveStock(productId, quantity);
        if (!reserved) {
            throw new RuntimeException("Failed to reserve stock for product: " + productId);
        }

        // Step 4: Save the order
        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        Order order = Order.builder()
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .status(Order.OrderStatus.CONFIRMED)
                .notes(notes)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order placed successfully: orderId={}, total={}", saved.getId(), totalPrice);
        return saved;
    }

    /**
     * Cancels an order and releases reserved stock.
     * Workflow:
     *   1. Fetch & validate order
     *   2. Release stock            → calls Product Service
     *   3. Update order status
     */
    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled.");
        }
        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel a delivered order.");
        }

        // Release stock back to Product Service
        productServiceClient.releaseStock(order.getProductId(), order.getQuantity());

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        Order updated = orderRepository.save(order);
        log.info("Order {} cancelled", orderId);
        return updated;
    }

    /**
     * Advances order to the next status (CONFIRMED → SHIPPED → DELIVERED).
     */
    @Transactional
    public Order advanceOrderStatus(Long orderId) {
        Order order = getOrderById(orderId);
        switch (order.getStatus()) {
            case PENDING    -> order.setStatus(Order.OrderStatus.CONFIRMED);
            case CONFIRMED  -> order.setStatus(Order.OrderStatus.SHIPPED);
            case SHIPPED    -> order.setStatus(Order.OrderStatus.DELIVERED);
            default -> throw new RuntimeException(
                    "Cannot advance order in status: " + order.getStatus());
        }
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }
}
