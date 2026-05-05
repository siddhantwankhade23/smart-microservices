package com.upskill.smart.simulation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DriverSimulationRequest {
    private Long orderId;
    private Long driverId;
    private Double startLat;
    private Double startLng;
    private Double destinationLat;
    private Double destinationLng;
}
