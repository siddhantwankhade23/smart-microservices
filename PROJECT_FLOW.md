# Smart Microservices Project Flow

This document outlines the architecture and workflow of the Smart Microservices project.

## 1. Project Overview

The project is a microservices-based application simulating a logistics or ride-hailing platform. It consists of the following modules:

*   **`eureka-server`**: Service registry for all microservices.
*   **`api-gateway`**: The central entry point for all external requests, handling routing and security (JWT).
*   **`auth-service`**: Manages user authentication and JWT token generation.
*   **`order-service`**: Manages the lifecycle of orders.
*   **`driver-service`**: Manages driver information, availability, and location tracking.
*   **`simulation-service`**: Simulates driver movement for accepted orders.
*   **`kafka-events`**: A shared module containing Kafka event definitions (DTOs).
*   **`notification-service`**: (Not yet implemented).

## 2. Services Details

### 2.1. `eureka-server`
*   **Purpose**: Netflix Eureka server for service discovery. All other services register here.
*   **Port**: `8761`

### 2.2. `api-gateway`
*   **Purpose**: Spring Cloud Gateway that routes external traffic to internal services and enforces JWT-based authentication.
*   **Port**: `8080`
*   **Routing**:
    *   `/auth/**` -> `auth-service`
    *   `/orders/**` -> `order-service`
    *   `/drivers/**` -> `driver-service`
*   **Security Configuration**:
    *   `/auth/register` and `/auth/login` are publicly accessible (`permitAll`).
    *   `/orders/**` requires the `CUSTOMER` role.
    *   All other requests must be authenticated.
    *   Uses a `JwtAuthenticationFilter` to validate tokens on incoming requests.

### 2.3. `auth-service`
*   **Purpose**: Handles user registration and login, generating JWTs for authenticated sessions.
*   **Endpoints**:
    *   `POST /auth/register`: Creates a new user (stores in DB with hashed password and role).
    *   `POST /auth/login`: Validates credentials and returns a JWT.

### 2.4. `order-service`
*   **Purpose**: Handles order creation and tracks order status.
*   **Port**: `8081`
*   **Database**: PostgreSQL (`logistics_db`)
*   **API Endpoints**:
    *   `POST /orders`: Creates a new order.
    *   `PATCH /orders/{orderId}/pickup`: Updates an order's status to `PICKED_UP`.
    *   `PATCH /orders/{orderId}/deliver`: Updates an order's status to `DELIVERED`.
*   **Kafka Integration**:
    *   **Produces**:
        *   `OrderPlacedEvent` (to `order-events` topic) upon order creation.
        *   `OrderPickedUpEvent` (to `order-events` topic) when marked as picked up.
    *   **Consumes**:
        *   Listens to `driver-events` for `DriverAssignedEvent` to update the order's driver ID and status to `ASSIGNED`.

### 2.5. `driver-service`
*   **Purpose**: Manages drivers, their availability, and live locations.
*   **Port**: `8082`
*   **Database**: PostgreSQL (`logistics_db`)
*   **API Endpoints**:
    *   `POST /drivers/location`: Updates a driver's coordinates in the database.
    *   `POST /drivers/availability`: Updates a driver's `isAvailable` status.
*   **Kafka Integration**:
    *   **Consumes**:
        *   Listens to `order-events` for `OrderPlacedEvent` to find the nearest available driver.
    *   **Produces**:
        *   `DriverAssignedEvent` (to `driver-events` topic) once a driver is matched to an order.
        *   `DriverLocationUpdateEvent` (to `driver-events` topic) when a driver's location is updated via API.

### 2.6. `simulation-service`
*   **Purpose**: Programmatically simulates a driver moving from a start to a destination coordinate.
*   **Port**: `8083`
*   **Kafka Integration**:
    *   **Consumes**:
        *   Listens to `order-events` for `OrderPickedUpEvent` to begin the simulation.
*   **Functionality**:
    *   Upon consuming `OrderPickedUpEvent`, it iteratively calculates the driver's next coordinate.
    *   At each step, it makes a synchronous API call to `driver-service` (`POST /drivers/location`).
    *   Upon reaching the destination, it makes API calls to `order-service` (`PATCH /orders/{id}/deliver`) and `driver-service` (`POST /drivers/availability` to free the driver).

---

## 3. End-to-End Workflow

### Step 1: Authentication
1.  A user registers via `POST gateway:8080/auth/register` (routed to `auth-service`).
2.  The user logs in via `POST gateway:8080/auth/login` and receives a JWT token.
3.  The client must include this JWT as a Bearer token in the `Authorization` header for subsequent requests.

### Step 2: Order Creation
1.  An authenticated user (with role `CUSTOMER`) sends a `POST` request to `gateway:8080/orders` with pickup and drop-off coordinates.
2.  `api-gateway` validates the JWT and routes the request to `order-service`.
3.  `order-service` saves the order (Status: `CREATED`) and publishes an `OrderPlacedEvent` to the `order-events` Kafka topic.

### Step 3: Driver Matching
1.  `driver-service` consumes the `OrderPlacedEvent`.
2.  It queries its database for the nearest driver where `isAvailable = true`.
3.  It marks the selected driver as unavailable (`isAvailable = false`).
4.  It publishes a `DriverAssignedEvent` to the `driver-events` Kafka topic.

### Step 4: Order Assignment
1.  `order-service` consumes the `DriverAssignedEvent`.
2.  It updates the specific order in the database, setting the `driverId` and changing the status to `ASSIGNED`.

### Step 5: Pickup Simulation Trigger
1.  An API call is made to `gateway:8080/orders/{orderId}/pickup`.
2.  `order-service` changes the order status to `PICKED_UP`.
3.  `order-service` publishes an `OrderPickedUpEvent` to the `order-events` Kafka topic.

### Step 6: Driver Movement Simulation
1.  `simulation-service` consumes the `OrderPickedUpEvent`.
2.  It starts a loop, calculating micro-movements from the start coordinates to the destination coordinates.
3.  **For every micro-movement:**
    *   `simulation-service` makes a synchronous REST call to `driver-service` (`POST /drivers/location`).
    *   `driver-service` updates the database with the new coordinates.
    *   `driver-service` publishes a `DriverLocationUpdateEvent` to the `driver-events` Kafka topic (this could be used by a future notification or websocket service to update the UI).

### Step 7: Delivery Completion
1.  When the loop in `simulation-service` reaches the final destination, movement stops.
2.  `simulation-service` makes a REST call to `order-service` (`PATCH /orders/{orderId}/deliver`). `order-service` updates the status to `DELIVERED`.
3.  `simulation-service` makes a REST call to `driver-service` (`POST /drivers/availability?isAvailable=true`). `driver-service` marks the driver as available for new orders.