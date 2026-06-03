package com.upskill.smart.driver;

import com.upskill.smart.kafka.events.DriverAssignedEvent;
import com.upskill.smart.kafka.events.DriverLocationUpdateEvent;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DriverEventsProducer {

    private static final String TOPIC = "driver-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendDriverAssignedEvent(DriverAssignedEvent driverAssignedEvent){
        kafkaTemplate.send(TOPIC, String.valueOf(driverAssignedEvent.orderId()), driverAssignedEvent)
                .whenComplete((driverAssignedEventSendResult, throwable) -> {

                    if (throwable == null){
                        RecordMetadata recordMetadata = driverAssignedEventSendResult.getRecordMetadata();
                        System.out.println("Driver Assigned Event Sent " + recordMetadata.offset() + " : "+recordMetadata.partition());
                    } else {
                        System.out.println("Failed to send Driver Assigned Event : "+throwable.getMessage());
                    }

                });
    }

    public void sendDriverLocationUpdateEvent(DriverLocationUpdateEvent driverLocationUpdateEvent) {

        kafkaTemplate.send(TOPIC, driverLocationUpdateEvent).whenComplete((
                stringObjectSendResult, throwable) -> {
            if (throwable == null){
                System.out.println("Driver Location Sent");
            } else {
                System.out.println("Driver Location Update Publish failed : "+throwable.getMessage());
            }
        });
    }
}
