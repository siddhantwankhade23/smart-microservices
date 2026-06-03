package com.upskill.smart.simulation;

import com.upskill.smart.simulation.dto.DriverSimulationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final DriverSimulationService driverSimulationService;

    @PostMapping("/driver/start")
    public ResponseEntity<String> startSimulation(
            @RequestBody DriverSimulationRequest request) {

        //driverSimulationService.simulateDriverMovement(request);

        return ResponseEntity.ok("Simulation started");
    }
}