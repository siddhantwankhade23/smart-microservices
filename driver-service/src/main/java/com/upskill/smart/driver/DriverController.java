package com.upskill.smart.driver;

import com.upskill.smart.driver.dto.UpdateDriverLocationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
@Slf4j
public class DriverController {
    private final DriverService driverService;

    @PostMapping("/location")
    public String updateLocation(@RequestBody UpdateDriverLocationRequest request) {
        driverService.updateLocation(
                request.getDriverId(),
                request.getLatitude(),
                request.getLongitude());
        log.info("Location Updated");
        return "Location Updated";
    }

    @PostMapping("/availability")
    public String updateAvailability(
            @RequestParam Long driverId,
            @RequestParam Boolean isAvailable
    ) {
        driverService.updateAvailability(driverId, isAvailable);
        log.info("Availability updated");
        return "Availability updated";
    }

//    @PostMapping("/assign")
//    public Long assignDriver(@RequestBody OrderPlacedEvent orderPlacedEvent){
//        return driverService.assignDriver(orderPlacedEvent.getPickupLatitude(), orderPlacedEvent.getPickupLongitude());
//    }
}
