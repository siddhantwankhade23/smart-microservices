package com.upskill.smart.simulation;

import com.upskill.smart.simulation.dto.DriverSimulationRequest;
import com.upskill.smart.simulation.dto.UpdateDriverLocationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class DriverSimulationService {

    @Async
    public void simulateDriverMovement(DriverSimulationRequest req) {

        double step = 0.001;

        double newLat = req.getStartLat();
        double newLng = req.getStartLng();

        double targetLat = req.getDestinationLat();
        double targetLng = req.getDestinationLng();

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

            System.out.println(newLat + "," + newLng);

            updateDriverLocation(req.getDriverId(), newLat, newLng);

//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
        }

        System.out.println("🚗 Driver reached destination");

        updateOrderStatus(req.getOrderId());
        updateAvailability(req.getDriverId(), true);
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

    public void updateDriverLocation(Long driverId, Double lat, Double lng) {

        WebClient client = WebClient.create("http://localhost:8082/drivers");

        client.post()
                .uri("/location")
                .bodyValue(new UpdateDriverLocationRequest(driverId, lat, lng))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void updateAvailability(Long driverId, boolean isAvailable) {

        WebClient client = WebClient.create("http://localhost:8082/drivers");

        client.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/availability")
                        .queryParam("driverId", driverId)
                        .queryParam("isAvailable", isAvailable)
                        .build())
                .retrieve();
    }

    private void updateOrderStatus(Long orderId) {
        WebClient client = WebClient.create("http://localhost:8081/orders");
        client.patch()
                .uri("/{id}/deliver", orderId)
                .retrieve()
                .bodyToMono(Void.class);
    }
}