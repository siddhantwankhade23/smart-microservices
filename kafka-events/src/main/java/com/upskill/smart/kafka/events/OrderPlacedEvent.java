package com.upskill.smart.kafka.events;


public record OrderPlacedEvent(

        Long orderId,
        Double pickupLatitude,
        Double pickupLongitude
) {
}

