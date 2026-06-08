package com.upskill.smart.driver;

import com.upskill.smart.kafka.events.DriverAssignedEvent;
import com.upskill.smart.kafka.events.DriverLocationUpdateEvent;
import com.upskill.smart.kafka.events.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@KafkaListener(topics = "order-events")
@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;

    private final DriverEventsProducer driverEventsProducer;

    public void updateLocation(Long driverId, Double lat, Double lon) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        driver.setLatitude(lat);
        driver.setLongitude(lon);

        publishDriverLocation(driverId, lat, lon);
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

        // Pauses the current thread for 2,000 milliseconds (2 seconds)
        dummyDelay(3000);

        return availableDrivers.stream()
                .min(Comparator.comparingDouble(driver ->
                        Math.sqrt(
                        Math.pow(driver.getLatitude() - pickupLat, 2) +
                                Math.pow(driver.getLongitude() - pickupLng, 2)
                )));
    }

    private static void dummyDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Restore interrupted status
            Thread.currentThread().interrupt();
        }
    }

    private void publishDriverLocation(Long driverId, double newLat, double newLng) {
        driverEventsProducer.sendDriverLocationUpdateEvent(new DriverLocationUpdateEvent(driverId, newLat, newLng));
    }

    @KafkaHandler
    public void assignDriver(OrderPlacedEvent orderPlacedEvent) {
        log.info("Order Placed Event Received for Order : "+orderPlacedEvent.orderId());
        Optional<Driver> optionalDriver = findNearestAvailableDriver(orderPlacedEvent.pickupLatitude(), orderPlacedEvent.pickupLongitude());

        Driver driver = optionalDriver.orElseThrow(() -> new RuntimeException("No available driver found"));
        driver.setIsAvailable(false);
        driverRepository.save(driver);

        driverEventsProducer.sendDriverAssignedEvent(new DriverAssignedEvent(driver.getId(), orderPlacedEvent.orderId()));

        log.info("Driver Id : "+driver.getId()+ " Name : "+driver.getName()+" assigned for order ID : "+orderPlacedEvent.orderId());
    }

    @KafkaHandler(isDefault = true)
    public void ignoreEvents(Object event) {
        log.info("Ignoring " + event.getClass().getSimpleName());
    }
}
