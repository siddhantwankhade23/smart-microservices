package com.upskill.smart.order;

import com.upskill.smart.kafka.events.DriverAssignedEvent;
import com.upskill.smart.kafka.events.OrderPlacedEvent;
import com.upskill.smart.order.dto.DriverSimulationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@KafkaListener(topics = "driver-events")
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
        log.info("Order Saved");
        sendOrderPlacedEvent(savedOrder);
        //processOrderAssignment(savedOrder);
        return savedOrder;
    }

    private void sendOrderPlacedEvent(Order savedOrder){
        orderEventProducer.sendOrderPlacedEvent(savedOrder);
    }

    private void processOrderAssignment(Order order) {
        log.info("processOrderAssignment");

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
        log.info("2");
    }

    @KafkaHandler
    public void processOrderPickup(DriverAssignedEvent driverAssignedEvent) {
        log.info("Driver Assigned Event Received for Order : "+driverAssignedEvent.orderId() + " - Driver Assigned : "+driverAssignedEvent.driverId());
        Long orderId = driverAssignedEvent.orderId();
        Long driverId = driverAssignedEvent.driverId();

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

        order.setDriverId(driverId);
        order.setStatus(OrderStatus.ASSIGNED);
        orderRepository.save(order);
        log.info("2");
    }

    @KafkaHandler(isDefault = true)
    public void ignoreEvents(Object event) {
        log.info("Ignoring " + event.getClass().getSimpleName());
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////
    //APIs for Simulating Order Picked up and Order delivered Behaviour

    public Order markOrderAsPickedUp(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        // Update status
        order.setStatus(OrderStatus.PICKED_UP);
        Order pickedUpOrder = orderRepository.save(order);

        sendOrderPickedUpEvent(pickedUpOrder);

        //handlePostPickup(pickedUpOrder);
        return pickedUpOrder;
    }

    private void sendOrderPickedUpEvent(Order pickedUpOrder){
        orderEventProducer.sendOrderPickedUpEvent(pickedUpOrder);
    }


    private void handlePostPickup(Order order) {
        log.info("Start Simulation for order: " + order.getId());

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
