package com.example.orderservice.controller;

import com.example.orderservice.model.Order;
import com.example.orderservice.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    /**
     * Place a new order.
     * Internally calls User Service (validate user) and Product Service (reserve stock).
     */
    @PostMapping
    public ResponseEntity<Order> placeOrder(@RequestBody PlaceOrderRequest request) {
        Order order = orderService.placeOrder(
                request.getUserId(),
                request.getProductId(),
                request.getQuantity(),
                request.getNotes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Cancel an order and release stock back to Product Service.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    /**
     * Advance the order through statuses: CONFIRMED → SHIPPED → DELIVERED.
     */
    @PostMapping("/{id}/advance")
    public ResponseEntity<Order> advanceStatus(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.advanceOrderStatus(id));
    }

    @Data
    public static class PlaceOrderRequest {
        private Long userId;
        private Long productId;
        private Integer quantity;
        private String notes;
    }
}
