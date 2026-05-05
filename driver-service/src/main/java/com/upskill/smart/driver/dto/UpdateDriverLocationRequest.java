package com.upskill.smart.driver.dto;

import lombok.Data;

@Data
public class UpdateDriverLocationRequest {
    private Long driverId;
    private Double latitude;
    private Double longitude;
}