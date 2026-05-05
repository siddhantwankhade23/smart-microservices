package com.upskill.smart.order;

import com.upskill.smart.order.dto.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private static final String TOPIC = "order-events";
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void sendOrderPlacedEvent(Order savedOrder) {
        try {

            CompletableFuture<SendResult<String, OrderPlacedEvent>> orderPlacedEvent =
                    kafkaTemplate.send(TOPIC, String.valueOf(savedOrder.getId()),
                            new OrderPlacedEvent(savedOrder.getId(), savedOrder.getPickupLat(), savedOrder.getPickupLng()));

            orderPlacedEvent
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            System.out.println("Sent successfully");
                        } else {
                            System.out.println("Failed: " + ex.getMessage());
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}