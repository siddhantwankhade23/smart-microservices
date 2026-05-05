package com.upskill.smart.driver;

import jakarta.persistence.*;
import lombok.Data;


@Data
@Table(name = "drivers")
@Entity
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String phoneNumber;

    private Double latitude;

    private Double longitude;

    private Boolean isAvailable;

    private String area;
}
