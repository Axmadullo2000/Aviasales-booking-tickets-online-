# Booking tickets (Aviasales-style)

A high-performance **Booking & Delivery Platform** built with **Java 21**, **Spring Boot 3.2+**, and **microservice architecture**. The system is designed to handle high-scale booking, event-driven workflows, and real-time flight or delivery search capabilities. It leverages modern patterns such as **Event Sourcing**, **CQRS**, and **DDD** to ensure maintainability, scalability, and flexibility.

## Tech Stack

- **Backend**: Java 21 LTS, Spring Boot 3.2+
- **ORM**: Spring Data JPA, Hibernate 6
- **Databases**: PostgreSQL 15+ (with PostGIS for geospatial queries), Redis for caching
- **Security**: Spring Security 6, OAuth2, JWT, Keycloak (for SSO)

## Core Features

1. **User Service**:
    - User registration, login, and authentication with JWT tokens.
    - Integration with **Keycloak** for SSO (optional).

## Getting Started

### Prerequisites

To run the application locally, you need the following software installed:

- **Java 21 LTS** or newer
- **Maven** (for building the project)
- **Docker** (for containerization and dependencies)
- **PostgreSQL 15+** (for database)

### Setup

1. **Clone the repository**:

   ```bash
   git clone https://github.com/Axmadullo2000/Aviasales-booking-tickets-online-
