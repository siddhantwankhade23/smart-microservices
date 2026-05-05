package com.upskill.smart.order;

import com.upskill.smart.order.dto.OrderPlacedEvent;
import com.upskill.smart.order.dto.DriverSimulationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final WebClient.Builder webClientBuilder;
    private final OrderEventProducer orderEventProducer;

    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setPickupAddress(request.getPickupLocation());
        order.setDropAddress(request.getDropLocation());
        order.setPickupLat(request.getPickupLat());
        order.setPickupLng(request.getPickupLng());
        order.setDropLat(request.getDropLat());
        order.setDropLng(request.getDropLng());
        order.setStatus(OrderStatus.CREATED);
        Order savedOrder = orderRepository.save(order);
        System.out.println("Order Saved");
        sendOrderPlacedEvent(savedOrder);
        //processOrderAssignment(savedOrder);


        return savedOrder;
    }

    private void sendOrderPlacedEvent(Order savedOrder){
        orderEventProducer.sendOrderPlacedEvent(savedOrder);
    }

    private void processOrderAssignment(Order order) {
        System.out.println("processOrderAssignment");

        WebClient client = webClientBuilder.build();

        Long driverId = client.post()
                .uri("http://driver-service/drivers" + "/assign")
                .bodyValue(new OrderPlacedEvent(order.getId(),order.getPickupLat(), order.getPickupLng()))
                .retrieve()
                .bodyToMono(Long.class)
                .block(); // makes it behave like RestTemplate

        order.setDriverId(driverId);
        order.setStatus(OrderStatus.ASSIGNED);
        orderRepository.save(order);
        System.out.println("2");
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////
    //APIs for Simulating Order Picked up and Order delivered Behaviour

    public Order markOrderAsPickedUp(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        // Update status
        order.setStatus(OrderStatus.PICKED_UP);
        Order pickedUpOrder = orderRepository.save(order);
        handlePostPickup(pickedUpOrder);
        return pickedUpOrder;
    }

    private void handlePostPickup(Order order) {
        System.out.println("Start Simulation for order: " + order.getId());
        WebClient client = webClientBuilder.build();
        client.post()
                .uri("http://simulation-service/simulation" + "/driver/start")
                .bodyValue(new DriverSimulationRequest(
                        order.getId(),
                        order.getDriverId(),
                        order.getPickupLat(),
                        order.getPickupLng(),
                        order.getDropLat(),
                        order.getDropLng()))
                .retrieve()
                .bodyToMono(Void.class).subscribe();
    }

    public Order markOrderAsDelivered(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        // Update status
        order.setStatus(OrderStatus.DELIVERED);
        return orderRepository.save(order);
    }






}
