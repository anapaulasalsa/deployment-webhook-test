package com.test.dynatrace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * GET /api/health - Simple health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    /**
     * GET /api/orders - List all orders
     */
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> listOrders() {
        return ResponseEntity.ok(Map.of(
            "total", orderService.getAllOrders().size(),
            "orders", orderService.getAllOrders()
        ));
    }

    /**
     * GET /api/orders/{id} - Get a specific order
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        Order order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    /**
     * POST /api/orders - Create a new order
     */
    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(
            @RequestParam String customerName,
            @RequestParam Double amount) {
        Order order = orderService.createOrder(customerName, amount);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * PUT /api/orders/{id}/status - Update order status
     */
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<Order> updateStatus(
            @PathVariable String orderId,
            @RequestParam String status) {
        Order order = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(order);
    }

    /**
     * POST /api/orders/{id}/process - Process an order
     */
    @PostMapping("/orders/{orderId}/process")
    public ResponseEntity<Order> processOrder(@PathVariable String orderId) {
        Order order = orderService.processOrder(orderId);
        return ResponseEntity.ok(order);
    }

    /**
     * DELETE /api/orders/{id} - Delete an order
     */
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, String>> deleteOrder(@PathVariable String orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.ok(Map.of("message", "Order deleted successfully"));
    }

    /**
     * POST /api/trigger-error - Intentionally trigger an error
     */
    @PostMapping("/trigger-error")
    public ResponseEntity<Map<String, String>> triggerError(
            @RequestParam(required = false, defaultValue = "500") int statusCode,
            @RequestParam(required = false, defaultValue = "Intentional error triggered for testing") String message) {
        
        throw new RuntimeException(message);
    }

    /**
     * POST /api/trigger-error/batch - Trigger multiple errors in sequence
     */
    @PostMapping("/trigger-error/batch")
    public ResponseEntity<Map<String, Object>> triggerErrorBatch(
            @RequestParam(required = false, defaultValue = "5") int count) {
        
        Map<String, Object> results = new HashMap<>();
        int successful = 0;
        
        for (int i = 0; i < count; i++) {
            try {
                // Try to get a non-existent order to trigger an error
                orderService.getOrderById("NON-EXISTENT-" + i);
                successful++;
            } catch (RuntimeException e) {
                // Expected - count it
            }
        }
        
        results.put("total_attempts", count);
        results.put("errors_triggered", count - successful);
        
        // Trigger one more to actually fail the request
        throw new RuntimeException("Batch error injection complete with " + (count - successful) + " errors");
    }

    /**
     * GET /api/errors/config - Get error injection configuration
     */
    @GetMapping("/errors/config")
    public ResponseEntity<Map<String, Object>> getErrorConfig() {
        return ResponseEntity.ok(orderService.getErrorStatus());
    }

    /**
     * POST /api/errors/enable - Enable random error injection
     */
    @PostMapping("/errors/enable")
    public ResponseEntity<Map<String, Object>> enableErrors(
            @RequestParam(required = false, defaultValue = "30") int errorRate) {
        
        orderService.setErrorInjection(true, errorRate);
        return ResponseEntity.ok(Map.of(
            "message", "Error injection enabled",
            "errorRate", errorRate + "%"
        ));
    }

    /**
     * POST /api/errors/disable - Disable random error injection
     */
    @PostMapping("/errors/disable")
    public ResponseEntity<Map<String, String>> disableErrors() {
        orderService.setErrorInjection(false, 0);
        return ResponseEntity.ok(Map.of("message", "Error injection disabled"));
    }

    /**
     * Global exception handler
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "error", e.getClass().getSimpleName(),
            "message", e.getMessage(),
            "timestamp", System.currentTimeMillis()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "error", e.getClass().getSimpleName(),
            "message", e.getMessage(),
            "timestamp", System.currentTimeMillis()
        ));
    }
}
