package com.test.dynatrace;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

@Service
public class OrderService {
    
    private final Map<String, Order> orders = new HashMap<>();
    private volatile boolean injectErrors = false;
    private volatile int errorRate = 0; // 0-100 percentage

    private static final Random random = new Random();

    public OrderService() {
        // Initialize with sample data
        orders.put("ORD-001", new Order("ORD-001", "John Doe", 150.50, "completed"));
        orders.put("ORD-002", new Order("ORD-002", "Jane Smith", 250.75, "pending"));
        orders.put("ORD-003", new Order("ORD-003", "Bob Johnson", 99.99, "completed"));
    }

    /**
     * Get all orders
     */
    public Map<String, Order> getAllOrders() {
        checkRandomErrorInjection();
        return new HashMap<>(orders);
    }

    /**
     * Get a specific order by ID
     */
    public Order getOrderById(String orderId) {
        checkRandomErrorInjection();
        
        if (!orders.containsKey(orderId)) {
            throw new RuntimeException("Order not found: " + orderId);
        }
        return orders.get(orderId);
    }

    /**
     * Create a new order
     */
    public Order createOrder(String customerName, Double amount) {
        checkRandomErrorInjection();
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order(orderId, customerName, amount, "pending");
        orders.put(orderId, order);
        
        return order;
    }

    /**
     * Update order status
     */
    public Order updateOrderStatus(String orderId, String newStatus) {
        checkRandomErrorInjection();
        
        if (!orders.containsKey(orderId)) {
            throw new RuntimeException("Order not found: " + orderId);
        }
        
        Order order = orders.get(orderId);
        order.setStatus(newStatus);
        
        return order;
    }

    /**
     * Process order (simulates a service operation)
     */
    public Order processOrder(String orderId) {
        checkRandomErrorInjection();
        
        Order order = getOrderById(orderId);
        
        // Simulate processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Order processing interrupted");
        }
        
        order.setStatus("processing");
        return order;
    }

    /**
     * Delete an order
     */
    public void deleteOrder(String orderId) {
        checkRandomErrorInjection();
        
        if (!orders.containsKey(orderId)) {
            throw new RuntimeException("Order not found: " + orderId);
        }
        orders.remove(orderId);
    }

    /**
     * Enable/disable random error injection
     */
    public void setErrorInjection(boolean enabled, int rate) {
        this.injectErrors = enabled;
        this.errorRate = Math.max(0, Math.min(100, rate)); // Clamp 0-100
    }

    /**
     * Get error injection status
     */
    public Map<String, Object> getErrorStatus() {
        return Map.of(
            "injectionEnabled", injectErrors,
            "errorRate", errorRate
        );
    }

    /**
     * Check if we should inject an error based on the error rate
     */
    private void checkRandomErrorInjection() {
        if (injectErrors && random.nextInt(100) < errorRate) {
            throw new RuntimeException("Simulated error injected by test harness");
        }
    }
}
