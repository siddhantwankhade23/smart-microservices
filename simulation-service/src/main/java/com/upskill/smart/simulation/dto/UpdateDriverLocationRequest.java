package com.upskill.smart.simulation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateDriverLocationRequest {
    private Long driverId;
    private Double latitude;
    private Double longitude;
}
