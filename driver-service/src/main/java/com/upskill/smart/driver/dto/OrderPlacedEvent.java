package com.upskill.smart.driver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPlacedEvent {

    private Long orderId;
    private Double pickupLatitude;
    private Double pickupLongitude;
}

