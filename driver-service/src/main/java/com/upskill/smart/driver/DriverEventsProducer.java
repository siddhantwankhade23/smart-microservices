package com.upskill.smart.driver;

import com.upskill.smart.kafka.events.DriverAssignedEvent;
import com.upskill.smart.kafka.events.DriverLocationUpdateEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
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
                        log.info("Driver Assigned Event Sent " + recordMetadata.offset() + " : "+recordMetadata.partition());
                    } else {
                        log.info("Failed to send Driver Assigned Event : "+throwable.getMessage());
                    }

                });
    }

    public void sendDriverLocationUpdateEvent(DriverLocationUpdateEvent driverLocationUpdateEvent) {

        kafkaTemplate.send(TOPIC, driverLocationUpdateEvent).whenComplete((
                stringObjectSendResult, throwable) -> {
            if (throwable == null){
                log.info("Driver Location Sent");
            } else {
                log.info("Driver Location Update Publish failed : "+throwable.getMessage());
            }
        });
    }
}
