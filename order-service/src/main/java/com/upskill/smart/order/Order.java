package com.upskill.smart.order;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pickupAddress;

    private String dropAddress;

    private Double pickupLat;

    private Double pickupLng;

    private Double dropLat;

    private Double dropLng;

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // CREATED, ASSIGNED, COMPLETED

    private Long driverId; // simple for now (later we’ll use relation)
}