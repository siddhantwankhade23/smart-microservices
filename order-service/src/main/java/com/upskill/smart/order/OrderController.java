package com.upskill.smart.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////
    //APIs for Simulating Order Picked up and Order delivered Behaviour
    @PatchMapping("/{orderId}/pickup")
    public ResponseEntity<Order> markOrderAsPickedUp(@PathVariable Long orderId){
        Order order = orderService.markOrderAsPickedUp(orderId);
        return ResponseEntity.ok(order);
    }

    @PatchMapping("/{orderId}/deliver")
    public ResponseEntity<Order> markOrderAsDelivered(@PathVariable Long orderId){
        Order order = orderService.markOrderAsDelivered(orderId);
        return ResponseEntity.ok(order);
    }

}
