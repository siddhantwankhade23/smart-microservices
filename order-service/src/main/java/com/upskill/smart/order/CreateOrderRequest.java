package com.upskill.smart.order;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private String pickupLocation;
    private String dropLocation;
    private Double pickupLat;
    private Double pickupLng;
    private Double dropLat;
    private Double dropLng;
}