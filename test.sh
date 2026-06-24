#!/bin/bash

# Quick test script for the deployment webhook test app
# Usage: ./test.sh [command]

BASE_URL="http://localhost:8080/api"
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}=== $1 ===${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Health check
test_health() {
    print_header "Health Check"
    curl -s $BASE_URL/health | jq .
    print_success "Health endpoint working"
}

# List orders
test_list_orders() {
    print_header "List All Orders"
    curl -s $BASE_URL/orders | jq .
    print_success "Orders listed"
}

# Create an order
test_create_order() {
    print_header "Create New Order"
    RESPONSE=$(curl -s -X POST "$BASE_URL/orders?customerName=TestUser&amount=150.50")
    echo $RESPONSE | jq .
    ORDER_ID=$(echo $RESPONSE | jq -r '.id')
    echo $ORDER_ID > /tmp/order_id.txt
    print_success "Order created: $ORDER_ID"
}

# Get specific order
test_get_order() {
    print_header "Get Specific Order"
    if [ -f /tmp/order_id.txt ]; then
        ORDER_ID=$(cat /tmp/order_id.txt)
        curl -s $BASE_URL/orders/$ORDER_ID | jq .
        print_success "Order retrieved"
    else
        print_error "No order ID found - create an order first"
    fi
}

# Process order
test_process_order() {
    print_header "Process Order"
    if [ -f /tmp/order_id.txt ]; then
        ORDER_ID=$(cat /tmp/order_id.txt)
        curl -s -X POST $BASE_URL/orders/$ORDER_ID/process | jq .
        print_success "Order processed"
    else
        print_error "No order ID found - create an order first"
    fi
}

# Update order status
test_update_status() {
    print_header "Update Order Status"
    if [ -f /tmp/order_id.txt ]; then
        ORDER_ID=$(cat /tmp/order_id.txt)
        curl -s -X PUT "$BASE_URL/orders/$ORDER_ID/status?status=completed" | jq .
        print_success "Order status updated"
    else
        print_error "No order ID found - create an order first"
    fi
}

# Trigger single error
test_single_error() {
    print_header "Trigger Single Error"
    curl -s -X POST $BASE_URL/trigger-error | jq . || echo "Error triggered (expected)"
    print_success "Single error triggered"
}

# Trigger batch errors
test_batch_errors() {
    print_header "Trigger Batch Errors"
    COUNT=${1:-5}
    curl -s -X POST "$BASE_URL/trigger-error/batch?count=$COUNT" | jq . || echo "Batch error triggered (expected)"
    print_success "Batch errors triggered (count: $COUNT)"
}

# Enable random errors
test_enable_errors() {
    print_header "Enable Random Error Injection"
    RATE=${1:-30}
    curl -s -X POST "$BASE_URL/errors/enable?errorRate=$RATE" | jq .
    print_success "Error injection enabled ($RATE%)"
}

# Disable random errors
test_disable_errors() {
    print_header "Disable Random Error Injection"
    curl -s -X POST $BASE_URL/errors/disable | jq .
    print_success "Error injection disabled"
}

# Get error config
test_error_config() {
    print_header "Error Configuration Status"
    curl -s $BASE_URL/errors/config | jq .
}

# Run a load test (multiple requests)
test_load() {
    print_header "Load Test (50 requests)"
    for i in {1..50}; do
        curl -s -X POST "$BASE_URL/orders?customerName=LoadTest&amount=$((RANDOM % 1000))" > /dev/null
        echo -ne "Completed: $i/50\r"
    done
    echo -e "\nLoad test completed"
    print_success "50 orders created"
}

# Menu
show_menu() {
    echo ""
    echo "Deployment Webhook Test App - Quick Test Script"
    echo ""
    echo "1. Health Check"
    echo "2. List Orders"
    echo "3. Create Order"
    echo "4. Get Order"
    echo "5. Process Order"
    echo "6. Update Status"
    echo "7. Trigger Single Error"
    echo "8. Trigger Batch Errors"
    echo "9. Enable Random Errors (30%)"
    echo "10. Disable Random Errors"
    echo "11. Check Error Config"
    echo "12. Load Test (50 orders)"
    echo "13. Full Test Sequence"
    echo "0. Exit"
    echo ""
}

# Full test sequence
test_full_sequence() {
    print_header "FULL TEST SEQUENCE"
    test_health
    echo ""
    test_list_orders
    echo ""
    test_create_order
    echo ""
    test_get_order
    echo ""
    test_process_order
    echo ""
    test_update_status
    echo ""
    test_enable_errors
    echo ""
    test_list_orders  # This might error due to injection
    echo ""
    test_disable_errors
    echo ""
    test_single_error
    echo ""
    print_header "Full sequence complete!"
}

# Main
if [ -z "$1" ]; then
    # Interactive mode
    while true; do
        show_menu
        read -p "Select option: " choice
        echo ""
        case $choice in
            1) test_health ;;
            2) test_list_orders ;;
            3) test_create_order ;;
            4) test_get_order ;;
            5) test_process_order ;;
            6) test_update_status ;;
            7) test_single_error ;;
            8) test_batch_errors ;;
            9) test_enable_errors ;;
            10) test_disable_errors ;;
            11) test_error_config ;;
            12) test_load ;;
            13) test_full_sequence ;;
            0) echo "Exiting..."; exit 0 ;;
            *) echo "Invalid option" ;;
        esac
        read -p "Press Enter to continue..."
    done
else
    # Direct command mode
    case $1 in
        health) test_health ;;
        orders) test_list_orders ;;
        create) test_create_order ;;
        get) test_get_order ;;
        process) test_process_order ;;
        status) test_update_status ;;
        error) test_single_error ;;
        batch) test_batch_errors $2 ;;
        enable) test_enable_errors $2 ;;
        disable) test_disable_errors ;;
        config) test_error_config ;;
        load) test_load ;;
        full) test_full_sequence ;;
        *) echo "Unknown command: $1"; exit 1 ;;
    esac
fi
