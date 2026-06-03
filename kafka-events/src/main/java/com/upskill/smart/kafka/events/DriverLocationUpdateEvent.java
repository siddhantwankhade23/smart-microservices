package com.upskill.smart.kafka.events;

public record DriverLocationUpdateEvent(Long driverId, double latitude, double longitude) {
}
