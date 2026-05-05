package com.upskill.smart.driver;

import com.upskill.smart.driver.dto.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;

    public void updateLocation(Long driverId, Double lat, Double lon) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        driver.setLatitude(lat);
        driver.setLongitude(lon);

        driverRepository.save(driver);
    }

    public void updateAvailability(Long driverId, Boolean isAvailable) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        driver.setIsAvailable(isAvailable);

        driverRepository.save(driver);
    }

    private Optional<Driver> findNearestAvailableDriver(Double pickupLat, Double pickupLng) {
        List<Driver> availableDrivers = driverRepository.findByIsAvailableTrue();

        return availableDrivers.stream()
                .min(Comparator.comparingDouble(driver ->
                        Math.sqrt(
                        Math.pow(driver.getLatitude() - pickupLat, 2) +
                                Math.pow(driver.getLongitude() - pickupLng, 2)
                )));
    }

    @KafkaListener(id = "order-placed-event-listener", topics = "order-events")
    public void assignDriver(OrderPlacedEvent orderPlacedEvent) {
        Optional<Driver> optionalDriver = findNearestAvailableDriver(orderPlacedEvent.getPickupLatitude(), orderPlacedEvent.getPickupLongitude());

        Driver driver = optionalDriver.orElseThrow(() -> new RuntimeException("No available driver found"));
        driver.setIsAvailable(false);
        driverRepository.save(driver);

        System.out.println("Driver Id : "+driver.getId()+ " Name : "+driver.getName()+" assigned for order ID : "+orderPlacedEvent.getOrderId());
    }


}
