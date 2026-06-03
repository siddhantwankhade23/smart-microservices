# Smart Microservices Project Flow

This document outlines the architecture and workflow of the Smart Microservices project.

## 1. Project Overview

The project is a microservices-based application that simulates a ride-hailing or delivery service. It consists of the following modules:

*   **`eureka-server`**: A service registry for all microservices.
*   **`order-service`**: Manages the lifecycle of orders.
*   **`driver-service`**: Manages driver information, availability, and location tracking.
*   **`simulation-service`**: Simulates driver movement for orders.
*   **`kafka-events`**: A shared module containing all Kafka event definitions (Data Transfer Objects).
*   **`notification-service`**: (Module created, but currently empty/not implemented).

## 2. Services

### 2.1. `eureka-server`
*   **Purpose**: Service registry allowing services to discover each other dynamically.
*   **Port**: `8761`

### 2.2. `order-service`
*   **Purpose**: Handles order creation and status updates.
*   **Port**: `8081`
*   **Database**: PostgreSQL (`logistics_db`)
*   **API Endpoints**:
    *   `POST /orders`: Creates a new order.
    *   `PATCH /orders/{orderId}/pickup`: Updates an order's status to `PICKED_UP`.
    *   `PATCH /orders/{orderId}/deliver`: Updates an order's status to `DELIVERED`.
*   **Kafka Integration**:
    *   **Produces to `order-events` topic**:
        *   `OrderPlacedEvent`: Emitted when an order is created.
        *   `OrderPickedUpEvent`: Emitted when an order is marked as picked up via API.
    *   **Consumes from `driver-events` topic**:
        *   Listens for `DriverAssignedEvent` to link a driver to an order and update status to `ASSIGNED`.

### 2.3. `driver-service`
*   **Purpose**: Manages drivers, their current locations, and assigns them to orders.
*   **Port**: `8082`
*   **Database**: PostgreSQL (`logistics_db`)
*   **API Endpoints**:
    *   `POST /drivers/location`: Updates a driver's latitude/longitude in the database and triggers a location update event.
    *   `POST /drivers/availability`: Updates a driver's `isAvailable` flag.
*   **Kafka Integration**:
    *   **Consumes from `order-events` topic**:
        *   Listens for `OrderPlacedEvent` to find the nearest available driver and assign them.
    *   **Produces to `driver-events` topic**:
        *   `DriverAssignedEvent`: Emitted after a driver is assigned to an order.
        *   `DriverLocationUpdateEvent`: Emitted whenever a driver's location is updated via the `/drivers/location` API.

### 2.4. `simulation-service`
*   **Purpose**: Automates the movement of a driver from pickup to drop-off to simulate real-world behavior.
*   **Port**: `8083`
*   **Kafka Integration**:
    *   **Consumes from `order-events` topic**:
        *   Listens for `OrderPickedUpEvent` to begin the movement simulation.
*   **Functionality**:
    *   Upon receiving an `OrderPickedUpEvent`, it enters a loop to incrementally change coordinates from start to destination.
    *   For every step in the loop, it makes a synchronous API call to `driver-service` (`POST /drivers/location`) to record the new position.
    *   When the destination is reached, it makes synchronous API calls to finalize the trip:
        *   `PATCH /orders/{id}/deliver` (on `order-service`)
        *   `POST /drivers/availability` (on `driver-service`)

## 3. Detailed End-to-End Workflow

1.  **Service Startup**: All active microservices boot up and register their host/port with `eureka-server`.

2.  **Order Initialization**:
    *   A client invokes `POST /orders` on `order-service` providing pickup and drop-off coordinates.
    *   `order-service` persists the new order with a status of `CREATED`.
    *   `order-service` publishes an `OrderPlacedEvent` to the `order-events` Kafka topic.

3.  **Driver Matching**:
    *   `driver-service` receives the `OrderPlacedEvent` from the `order-events` topic.
    *   It queries its database for available drivers and calculates the nearest one to the pickup location.
    *   It marks the chosen driver as unavailable (`isAvailable = false`) in its database.
    *   It publishes a `DriverAssignedEvent` (containing driverId and orderId) to the `driver-events` Kafka topic.

4.  **Order Acknowledgement**:
    *   `order-service` receives the `DriverAssignedEvent` from the `driver-events` topic.
    *   It updates the corresponding order in its database with the `driverId` and changes the status to `ASSIGNED`.

5.  **Simulating Pickup**:
    *   A client (or an external trigger) invokes `PATCH /orders/{orderId}/pickup` on `order-service`.
    *   `order-service` updates the order status to `PICKED_UP`.
    *   `order-service` publishes an `OrderPickedUpEvent` (containing full route details) to the `order-events` topic.

6.  **Driver Movement & Tracking (Simulation)**:
    *   `simulation-service` receives the `OrderPickedUpEvent` from the `order-events` topic.
    *   It begins calculating intermediate coordinates between the pickup and drop-off points.
    *   For every calculated step, `simulation-service` sends a synchronous REST request to `driver-service` at `POST /drivers/location`.
    *   `driver-service` updates the database with the new coordinates.
    *   Crucially, `driver-service` then publishes a `DriverLocationUpdateEvent` to the `driver-events` topic, broadcasting the real-time location.

7.  **Order Completion**:
    *   Once the simulation loop in `simulation-service` reaches the destination coordinates, the movement stops.
    *   `simulation-service` sends a synchronous REST request to `order-service` at `PATCH /orders/{orderId}/deliver`. `order-service` updates the status to `DELIVERED`.
    *   `simulation-service` sends a synchronous REST request to `driver-service` at `POST /drivers/availability` (setting `isAvailable = true`), freeing the driver for the next order.

This end-to-end trace reflects the hybrid approach of the system, utilizing asynchronous Kafka events for core state changes and synchronous REST API calls for continuous location updates and finalization steps driven by the simulator.