git commit -m "feat: implement complete booking system with flights and passengers

Features:
- Add booking creation with multiple flights and passengers
- Implement booking details retrieval with proper eager loading
- Add user bookings list with pagination
- Implement booking expiration scheduler
- Add booking cancellation functionality

Technical improvements:
- Fix MultipleBagFetchException using two separate JOIN FETCH queries
- Implement custom AuthenticatedUser for Spring Security integration
- Add @PreAuthorize for endpoint security
- Use @AuthenticationPrincipal to get current user context
- Prevent N+1 queries with optimized data fetching

Security:
- JWT-based authentication with user context
- Role-based authorization (USER, ADMIN)
- Verify booking ownership before access"
  Или короче:
  bashgit commit -m "feat: implement flight booking system

- Add booking creation, retrieval, and cancellation
- Implement user bookings pagination
- Fix MultipleBagFetchException with split queries
- Integrate JWT authentication with @AuthenticationPrincipal
- Add automated booking expiration scheduler"
  Или совсем краткий:
  bashgit commit -m "feat: add complete booking management system with authentication"
  Какой стиль вам больше подходит? 😊# Booking tickets (Aviasales-style)

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
   git clone https://github.com/Axmadullo2000/Aviasales-booking-tickets-online-Дополни этот Readme.mdShow more18:30markdown# Booking tickets (Aviasales-style)

A high-performance **Flight Booking Platform** built with **Java 21**, **Spring Boot 3.2+**, and modern enterprise patterns. The system provides comprehensive flight search, booking management, and user authentication with JWT-based security.

## Tech Stack

### Backend
- **Java 21 LTS** - Latest long-term support version
- **Spring Boot 3.2+** - Modern Spring framework
- **Spring Security 6** - JWT-based authentication & authorization
- **Spring Data JPA** - Data persistence layer
- **Hibernate 6** - ORM with advanced query optimization

### Databases
- **PostgreSQL 15+** - Primary relational database
- **Redis** - Caching layer for improved performance

### Documentation & API
- **Swagger/OpenAPI 3.0** - Interactive API documentation
- **Spring REST Docs** - API documentation generation

### Build & Deployment
- **Maven** - Dependency management and build tool
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration

## Core Features

### 1. Authentication & Authorization
- JWT-based authentication with access and refresh tokens
- Role-based access control (USER, ADMIN)
- Secure password encryption with BCrypt
- Token expiration and renewal mechanism
- Custom `@PreAuthorize` security annotations

### 2. Flight Management
- Flight search with multiple filters (origin, destination, date, class)
- Airport and airline information retrieval
- Popular destinations recommendations
- Flight details with pricing by seat class (Economy, Business, First Class)
- Real-time availability tracking

### 3. Booking System
- Create bookings with multiple flights and passengers
- Automated booking reference generation (6-character unique code)
- Booking status management (PENDING, CONFIRMED, CANCELLED, EXPIRED)
- User booking history with pagination
- Booking expiration scheduler (auto-cancel after 15 minutes if not confirmed)
- Optimized query fetching to prevent N+1 problems

### 4. Passenger Management
- Multiple passengers per booking
- Passenger information validation (passport, DOB, nationality)
- Passenger-flight association tracking

### 5. Performance Optimization
- Two-query strategy to avoid Hibernate MultipleBagFetchException
- Entity Graph for controlled eager loading
- Redis caching for frequently accessed data
- Optimized JOIN FETCH queries
- Pagination support for large datasets

## Architecture Highlights

### Security Architecture
```
Client Request → JWT Filter → SecurityContext → @PreAuthorize → Controller
                     ↓
              Token Validation
                     ↓
         AuthenticatedUser (Principal)
```

### Data Fetching Strategy
```java
// Optimized approach to avoid Cartesian product
Query 1: Booking + BookingFlights (JOIN FETCH)
Query 2: Booking + Passengers (JOIN FETCH)
Result: No N+1, No Cartesian Product
```

### Entity Relationships
```
User (1) ─────→ (N) Bookings
Booking (1) ───→ (N) BookingFlights ───→ (1) Flight
Booking (1) ───→ (N) Passengers
Flight (1) ─────→ (1) Airport (Departure)
Flight (1) ─────→ (1) Airport (Arrival)
Flight (1) ─────→ (1) Airline
```

## API Endpoints

### Authentication
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login (returns access & refresh tokens)
- `POST /api/v1/auth/refresh` - Refresh access token

### Flights
- `GET /api/flights/search` - Search flights with filters
- `GET /api/flights/{id}` - Get flight details
- `GET /api/flights/airports` - List all airports
- `GET /api/flights/airlines` - List all airlines
- `GET /api/flights/popular-destinations` - Get popular destinations

### Bookings (Authenticated)
- `POST /api/bookings` - Create new booking
- `GET /api/bookings` - Get user's bookings (paginated)
- `GET /api/bookings/{reference}` - Get booking details by reference
- `PUT /api/bookings/{reference}/cancel` - Cancel booking

## Getting Started

### Prerequisites

- **Java 21 LTS** or newer
- **Maven 3.8+**
- **Docker & Docker Compose**
- **PostgreSQL 15+**
- **Redis 7+** (optional, for caching)

### Environment Setup

1. **Clone the repository**:
```bash
   git clone https://github.com/Axmadullo2000/Aviasales-booking-tickets-online-
   cd Aviasales-booking-tickets-online-
```

2. **Configure database** (application.yml):
```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/booking_db
       username: your_username
       password: your_password
     jpa:
       hibernate:
         ddl-auto: update
       show-sql: true
```

3. **Set JWT Secret Key** (application.yml):
```yaml
   jwt:
     secret: your-256-bit-secret-key-here
     expiration: 900000 # 15 minutes
     refresh-expiration: 604800000 # 7 days
```

4. **Run with Docker Compose**:
```bash
   docker-compose up -d
```

5. **Build the application**:
```bash
   mvn clean install
```

6. **Run the application**:
```bash
   mvn spring-boot:run
```

### Docker Setup

**docker-compose.yml**:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: booking_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  booking-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/booking_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_REDIS_HOST: redis
    depends_on:
      - postgres
      - redis

volumes:
  postgres_data:
```

### Testing

Access the API documentation:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

### Sample Request

**Create Booking**:
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "flights": [
      {
        "flightId": 123,
        "seatClass": "ECONOMY"
      }
    ],
    "passengers": [
      {
        "firstName": "John",
        "lastName": "Doe",
        "passportNumber": "AB123456",
        "dateOfBirth": "1990-05-15",
        "nationality": "USA"
      }
    ]
  }'
```

## Project Structure
```
src/main/java/com/monolit/booking/
├── config/              # Security, Swagger, Redis configuration
├── controller/          # REST API endpoints
├── dto/                 # Data Transfer Objects
├── entity/              # JPA entities
├── exception/           # Custom exceptions & global handler
├── mapper/              # Entity-DTO mappers
├── projection/          # JPA projections
├── repository/          # Spring Data JPA repositories
├── security/            # JWT, filters, UserDetails
├── service/             # Business logic
│   └── impl/           # Service implementations
└── scheduler/          # Scheduled tasks (booking expiration)
```

## Key Technical Decisions

### 1. JWT Authentication
- **Why**: Stateless authentication for scalability
- **Implementation**: Custom JWT filter with Spring Security
- **Token Types**: Access (15 min) + Refresh (7 days)

### 2. Two-Query Fetching Strategy
- **Problem**: Hibernate MultipleBagFetchException with multiple Lists
- **Solution**: Separate queries for each collection
- **Benefit**: Avoids Cartesian product, maintains List semantics

### 3. Scheduled Booking Expiration
- **Why**: Auto-cleanup of unpaid bookings
- **Implementation**: Spring @Scheduled with cron expression
- **Interval**: Every 5 minutes checks for expired bookings

### 4. Pagination for User Bookings
- **Why**: Prevent performance issues with large result sets
- **Implementation**: Spring Data Pageable
- **Default**: 10 items per page, sorted by creation date

## Performance Optimizations

1. **Query Optimization**
   - Use `JOIN FETCH` to prevent N+1 queries
   - Split multiple collections into separate queries
   - Index on frequently queried fields (booking_reference, user_id)

2. **Caching Strategy**
   - Redis for flight search results (5-minute TTL)
   - Airport/Airline data cached (24-hour TTL)
   - JPA second-level cache for entities

3. **Database Indexes**
```sql
   CREATE INDEX idx_booking_reference ON bookings(booking_reference);
   CREATE INDEX idx_booking_user_id ON bookings(user_id);
   CREATE INDEX idx_flight_route_date ON flights(departure_airport_id, arrival_airport_id, departure_time);
```

## Future Enhancements

- [ ] Payment gateway integration (Stripe, PayPal)
- [ ] Email notifications for booking confirmations
- [ ] SMS notifications for flight updates
- [ ] Multi-language support (i18n)
- [ ] Admin dashboard for booking management
- [ ] Real-time flight status updates
- [ ] Loyalty program and rewards
- [ ] Microservices migration (User, Flight, Booking services)
- [ ] Event-driven architecture with Kafka
- [ ] GraphQL API for flexible queries

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

**Developer**: Axmadullo
**GitHub**: [@Axmadullo2000](https://github.com/Axmadullo2000)

---

⭐ If you find this project useful, please consider giving it a star!