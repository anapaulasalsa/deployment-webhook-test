package com.test.dynatrace;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Order {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("customer_name")
    private String customerName;
    
    @JsonProperty("amount")
    private Double amount;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("created_at")
    private Long createdAt;

    public Order() {}

    public Order(String id, String customerName, Double amount, String status) {
        this.id = id;
        this.customerName = customerName;
        this.amount = amount;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", customerName='" + customerName + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
