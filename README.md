# Smart Microservices

This project is a microservices-based application built with **Spring Boot** and **Spring Cloud**. It simulates a smart transportation or ride-hailing system, consisting of various decoupled services communicating through synchronous REST calls and asynchronous messaging.

## Architecture

The system consists of the following modules:

*   **eureka-server**: The service registry. It uses Netflix Eureka to allow other microservices to register themselves and discover each other dynamically without hardcoded hostnames or ports.
*   **order-service**: Responsible for managing orders/requests. It uses **PostgreSQL** for data persistence and **Apache Kafka** for asynchronous event-driven communication (e.g., broadcasting new orders or status updates). It exposes REST APIs using Spring WebMVC and can perform non-blocking HTTP calls using Spring WebFlux.
*   **driver-service**: Manages driver information, status, and availability. Like the order service, it uses **PostgreSQL** for storage and integrates with **Apache Kafka** to listen to or emit events.
*   **simulation-service**: Simulates system activities (e.g., simulating driver movements or order generation). It uses Spring WebFlux for reactive/non-blocking web requests.

## Technologies Used

*   **Java 17**
*   **Spring Boot** (Microservices framework)
*   **Spring Cloud Netflix Eureka** (Service Discovery)
*   **Apache Kafka** (Message Broker / Event Streaming)
*   **PostgreSQL** (Relational Database)
*   **Spring WebFlux** (Reactive web client/server)
*   **Lombok** (Boilerplate code reduction)
*   **Maven** (Dependency and build management)

## Getting Started

### Prerequisites

*   JDK 17 installed
*   Maven installed
*   A running instance of PostgreSQL
*   A running instance of Apache Kafka (and Zookeeper)

### Build and Run

1.  **Clone the repository.**
2.  **Build the project** from the root directory:
    ```bash
    mvn clean install
    ```
3.  **Run the Eureka Server** first, as other services depend on it for registration:
    ```bash
    cd eureka-server
    mvn spring-boot:run
    ```
4.  **Run the Order Service, Driver Service, and Simulation Service** in separate terminal windows:
    ```bash
    # In order-service directory
    mvn spring-boot:run
    
    # In driver-service directory
    mvn spring-boot:run
    
    # In simulation-service directory
    mvn spring-boot:run
    ```

*Note: Ensure your PostgreSQL and Kafka instances are running and that the `application.yml` files in the respective services are configured with the correct connection URLs and credentials.*
