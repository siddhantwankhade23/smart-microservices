package com.upskill.smart.kafka.events;

public record OrderPickedUpEvent(
        Long orderId,
        Long driverId,
        Double startLat,
        Double startLng,
        Double destinationLat,
        Double destinationLng
) {
}
