package com.upskill.smart.simulation;

import com.upskill.smart.kafka.events.OrderPickedUpEvent;
import com.upskill.smart.simulation.dto.UpdateDriverLocationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@KafkaListener(topics = "order-events")
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverSimulationService {

    private final WebClient.Builder webClientBuilder;

    @KafkaHandler
    public void simulateDriverMovement(OrderPickedUpEvent req) {

        double step = 0.01;

        double newLat = req.startLat();
        double newLng = req.startLng();

        double targetLat = req.destinationLat();
        double targetLng = req.destinationLng();

        while (!isNear(newLat, newLng, targetLat, targetLng)) {

            double dLat = targetLat - newLat;
            double dLng = targetLng - newLng;

            double distance = Math.sqrt(dLat * dLat + dLng * dLng);

            if (distance < step) {
                newLat = targetLat;
                newLng = targetLng;
            } else {
                newLat += (dLat / distance) * step;
                newLng += (dLng / distance) * step;
            }

            log.info(newLat + "," + newLng);

            updateDriverLocation(req.driverId(), newLat, newLng);

        }

        log.info("🚗 Driver reached destination");

        updateOrderStatus(req.orderId());
        updateAvailability(req.driverId(), true);
    }

    @KafkaHandler(isDefault = true)
    public void ignoreEvents(Object event) {
        log.info("Ignoring " + event.getClass().getSimpleName());
    }

    private double moveTowards(double current, double target, double step) {
        if (Math.abs(target - current) < step) return target;
        return current + (target > current ? step : -step);
    }

    private boolean isNear(double lat1, double lng1, double lat2, double lng2) {
        double threshold = 0.0005;
        return Math.abs(lat1 - lat2) < threshold &&
                Math.abs(lng1 - lng2) < threshold;
    }

    @LoadBalanced
    public void updateDriverLocation(Long driverId, Double lat, Double lng) {

        WebClient client = webClientBuilder.build();

        client.post()
                .uri("http://driver-service/drivers" + "/location")
                .bodyValue(new UpdateDriverLocationRequest(driverId, lat, lng))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void updateAvailability(Long driverId, boolean isAvailable) {

        WebClient client = webClientBuilder.build();

        client.post()
                .uri(uriBuilder -> uriBuilder
                        .path("http://driver-service/drivers" +"/availability")
                        .queryParam("driverId", driverId)
                        .queryParam("isAvailable", isAvailable)
                        .build())
                .retrieve();
    }

    private void updateOrderStatus(Long orderId) {
        WebClient client = webClientBuilder.build();
        client.patch()
                .uri("http://order-service" +"/{id}/deliver", orderId)
                .retrieve()
                .bodyToMono(Void.class);
    }
}