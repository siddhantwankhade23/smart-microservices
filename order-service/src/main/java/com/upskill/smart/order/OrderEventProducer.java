package com.upskill.smart.order;

import com.upskill.smart.kafka.events.OrderPickedUpEvent;
import com.upskill.smart.kafka.events.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private static final String TOPIC = "order-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderPlacedEvent(Order savedOrder) {
        try {

            CompletableFuture<SendResult<String, Object>> orderPlacedEvent =
                    kafkaTemplate.send(TOPIC, String.valueOf(savedOrder.getId()),
                            new OrderPlacedEvent(savedOrder.getId(), savedOrder.getPickupLat(), savedOrder.getPickupLng()));

            orderPlacedEvent
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Order Placed Event Sent successfully");
                        } else {
                            log.info("Failed: " + ex.getMessage());
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendOrderPickedUpEvent(Order pickedUpOrder) {

        try {

            CompletableFuture<SendResult<String, Object>> orderPickedUpEvent =
                    kafkaTemplate.send(TOPIC, String.valueOf(pickedUpOrder.getId()),
                            new OrderPickedUpEvent(
                                    pickedUpOrder.getId(), pickedUpOrder.getDriverId(),
                                    pickedUpOrder.getPickupLat(), pickedUpOrder.getPickupLng(),
                                    pickedUpOrder.getDropLat(), pickedUpOrder.getDropLng()));

            orderPickedUpEvent
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Order Pickup event sent successfully");
                        } else {
                            log.info("Failed: " + ex.getMessage());
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}