# ✈️ Aviasales - Flight Booking Platform

A production-ready **Flight Booking System** built with **Java 21**, **Spring Boot 3.2+**, and enterprise-grade architecture. Features comprehensive flight search, booking management, payment processing with support for Uzbekistan payment cards (UzCard, Humo), and realistic PDF ticket generation.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 🌟 Key Features

### 🔐 Authentication & Security
- **JWT-based authentication** with access and refresh tokens
- **Role-based authorization** (USER, ADMIN) with `@PreAuthorize`
- Secure password encryption with **BCrypt**
- Custom `AuthenticatedUser` principal for Spring Security integration
- Token expiration (15 min access, 7 days refresh)

### ✈️ Flight Management
- **Smart flight search** with multiple filters (route, date, class, passengers)
- **Real-time availability tracking** per seat class
- **Dynamic pricing** support (Economy, Business, First Class)
- Airport and airline comprehensive database
- Popular destinations with statistics
- Timezone-aware flight scheduling

### 📋 Booking System
- **Multi-passenger bookings** with automated reference generation
- **Booking lifecycle management** (PENDING → CONFIRMED → COMPLETED/CANCELLED)
- **15-minute expiration** for unpaid bookings with auto-cleanup scheduler
- **Optimized data fetching** preventing N+1 queries and Cartesian products
- Booking history with pagination
- Special requests and contact information support

### 💳 Payment Processing
- **Multi-card support**:
    - 🇺🇿 **UzCard** (8600)
    - 🇺🇿 **Humo** (9860)
    - 🌍 **Visa** (4)
    - 🌍 **MasterCard** (5, 2221-2720)
    - 🌍 **Maestro** (6)
    - 🇷🇺 **МИР** (2200-2204)
    - 🌍 **American Express** (34, 37)
    - 🇨🇳 **UnionPay** (62)
- **Smart card validation**:
    - Luhn algorithm for international cards
    - Custom validation for UzCard/Humo
    - Expiry date verification (MM/YY format)
    - CVV validation (3-4 digits based on card type)
- **Payment deduplication**:
    - Prevents multiple payments for single booking
    - Failed attempt tracking (max 5 attempts)
    - 30-minute cooldown after limit exceeded
    - Automatic cleanup of stuck PROCESSING payments
- **Transaction tracking** with unique IDs
- **Payment status management** (PROCESSING, COMPLETED, FAILED, REFUNDED)

### 🎫 Ticket Generation
- **Professional PDF tickets** in Uzbekistan Airways style
- **Boarding pass format** (21cm × 8cm)
- **Complete flight information**:
    - Passenger details with passport
    - Flight route and schedule
    - Seat assignment
    - Baggage allowance
    - E-ticket number with barcode
- **Multi-language support** (English, Russian, Uzbek)
- **Realistic design** matching actual airline tickets

### 👥 Passenger Management
- Complete passenger profiles (name, passport, DOB, nationality)
- Passport validation and expiry tracking
- Multiple passengers per booking
- Saved passenger profiles for repeat bookings
- Gender and nationality information

## 🏗️ Tech Stack

### Backend
```
Java 21 LTS          - Latest LTS with modern features
Spring Boot 3.2+     - Enterprise application framework
Spring Security 6    - Authentication & authorization
Spring Data JPA      - Data persistence abstraction
Hibernate 6          - Advanced ORM with query optimization
```

### Databases & Caching
```
PostgreSQL 15+       - Primary relational database
Redis 7+             - Caching layer (optional)
```

### Libraries & Tools
```
OpenPDF 1.3.30       - PDF generation for tickets
Lombok              - Boilerplate code reduction
MapStruct           - Entity-DTO mapping
Swagger/OpenAPI 3    - API documentation
```

### Build & DevOps
```
Maven 3.8+          - Dependency management
Docker              - Containerization
Docker Compose      - Multi-container orchestration
```

## 🚀 Getting Started

### Prerequisites

- **Java 21 LTS** ([Download](https://adoptium.net/))
- **Maven 3.8+** ([Download](https://maven.apache.org/))
- **Docker & Docker Compose** ([Download](https://www.docker.com/))
- **PostgreSQL 15+** (or use Docker)

### Quick Start with Docker

1. **Clone the repository**:
```bash
git clone https://github.com/Axmadullo2000/Aviasales-booking-tickets-online-.git
cd Aviasales-booking-tickets-online-
```

2. **Start services with Docker Compose**:
```bash
docker-compose up -d
```

3. **Access the application**:
    - API: http://localhost:8080
    - Swagger UI: http://localhost:8080/swagger-ui.html

### Manual Setup

1. **Configure Database** (`application.yml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/booking_db
    username: postgres
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

2. **Set JWT Configuration**:
```yaml
jwt:
  secret: your-256-bit-secret-key-must-be-very-long-and-secure
  expiration: 900000          # 15 minutes
  refresh-expiration: 604800000  # 7 days
```

3. **Build and Run**:
```bash
mvn clean install
mvn spring-boot:run
```

## 📡 API Endpoints

### Authentication
```http
POST   /api/v1/auth/register       - Register new user
POST   /api/v1/auth/login          - Login (returns JWT tokens)
POST   /api/v1/auth/refresh        - Refresh access token
```

### Flights (Public)
```http
GET    /api/flights/search         - Search flights
  ?from=TAS&to=DME&date=2026-03-15&passengers=2&cabinClass=ECONOMY

GET    /api/flights/{id}           - Get flight details
GET    /api/flights/airports       - List all airports
GET    /api/flights/airlines       - List all airlines
GET    /api/flights/popular        - Popular destinations
```

### Bookings (Authenticated)
```http
POST   /api/bookings               - Create new booking
GET    /api/bookings               - Get user's bookings (paginated)
GET    /api/bookings/{reference}   - Get booking details
DELETE /api/bookings/{reference}   - Cancel booking
GET    /api/bookings/{reference}/ticket - Download PDF ticket
```

### Payments (Authenticated)
```http
POST   /api/payments               - Create payment
GET    /api/payments/status/{transactionId}  - Check payment status
POST   /api/payments/confirm       - Confirm payment
POST   /api/payments/{id}/refund   - Refund payment
```

## 💡 Usage Examples

### 1. Register & Login
```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+998901234567"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePass123!"
  }'
```

### 2. Search Flights
```bash
curl "http://localhost:8080/api/flights/search?\
from=TAS&to=DME&date=2026-03-15&passengers=2&cabinClass=ECONOMY"
```

### 3. Create Booking
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "flightId": 123,
    "cabinClass": "ECONOMY",
    "passengers": [
      {
        "firstName": "Alisher",
        "lastName": "Mamadaliyev",
        "passportNumber": "AB1234567",
        "dateOfBirth": "1995-05-15",
        "nationality": "UZ",
        "gender": "MALE",
        "passportCountry": "UZ",
        "passportExpiry": "2030-12-31"
      }
    ],
    "contactEmail": "alisher@example.com",
    "contactPhone": "+998901234567",
    "specialRequests": "Window seat preferred"
  }'
```

### 4. Pay with UzCard
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "bookingReference": "ABC123",
    "amount": 17000.00,
    "paymentMethod": "CARD",
    "cardNumber": "8600123456781234",
    "expiryDate": "12/27",
    "cvv": "123"
  }'
```

### 5. Download Ticket
```bash
curl -X GET "http://localhost:8080/api/bookings/ABC123/ticket" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  --output ticket.pdf
```

## 🏛️ Architecture

### Project Structure
```
src/main/java/com/monolit/booking/
├── config/              # Application configuration
│   ├── SecurityConfig.java      # Spring Security setup
│   ├── SwaggerConfig.java       # API documentation
│   └── SchedulerConfig.java     # Scheduled tasks
├── controller/          # REST API endpoints
│   ├── AuthController.java
│   ├── FlightController.java
│   ├── BookingController.java
│   └── PaymentController.java
├── dto/                 # Data Transfer Objects
│   ├── request/        # Request DTOs
│   └── response/       # Response DTOs
├── entity/             # JPA entities
│   ├── User.java
│   ├── Flight.java
│   ├── Booking.java
│   ├── Ticket.java
│   ├── Payment.java
│   └── Passenger.java
├── enums/              # Enumerations
│   ├── BookingStatus.java
│   ├── PaymentStatus.java
│   ├── CabinClass.java
│   └── CardType.java
├── exception/          # Custom exceptions & handlers
│   ├── GlobalExceptionHandler.java
│   ├── BookingNotFoundException.java
│   └── PaymentProcessingException.java
├── mapper/             # Entity-DTO mappers
├── repository/         # Spring Data JPA repositories
├── security/           # Security components
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── AuthenticatedUser.java
├── service/            # Business logic
│   ├── interfaces/
│   └── impl/
│       ├── BookingServiceImpl.java
│       ├── PaymentServiceImpl.java
│       └── TicketPdfService.java
└── scheduler/          # Scheduled tasks
    └── BookingExpirationScheduler.java
```

### Entity Relationships
```
┌─────────┐
│  User   │────────────┐
└─────────┘            │
                       │ 1:N
                  ┌─────────┐
                  │ Booking │────────────┐
                  └─────────┘            │
                       │                 │ 1:N
                       │ 1:N        ┌─────────┐
                  ┌─────────┐       │ Ticket  │
                  │Passenger│       └─────────┘
                  └─────────┘            │
                                         │ N:1
                                    ┌─────────┐
                                    │ Flight  │
                                    └─────────┘
                                         │
                         ┌───────────────┼───────────────┐
                         │               │               │
                    ┌─────────┐     ┌─────────┐    ┌─────────┐
                    │Airport  │     │Airport  │    │ Airline │
                    │(Origin) │     │(Dest.)  │    └─────────┘
                    └─────────┘     └─────────┘
```

### Security Flow
```
Client Request
     ↓
JWT Authentication Filter
     ↓
Token Validation
     ↓
SecurityContext Population
     ↓
@PreAuthorize Check
     ↓
Controller Method
     ↓
@AuthenticationPrincipal
     ↓
Service Layer
```

### Payment Flow
```
Create Payment Request
     ↓
Validate Card (Type Detection)
     ↓
Check Existing Payments
     ↓
Validate Attempts Limit
     ↓
Create Payment (PROCESSING)
     ↓
Process Payment (Mock/Real Gateway)
     ↓
Update Status (COMPLETED/FAILED)
     ↓
Update Booking Status
     ↓
Generate Receipt & Ticket
```

## ⚡ Performance Optimizations

### 1. Query Optimization
```java
// Two-query strategy to avoid MultipleBagFetchException
// Query 1: Booking + Tickets
Booking booking = em.createQuery(
    "SELECT DISTINCT b FROM Booking b " +
    "LEFT JOIN FETCH b.tickets " +
    "WHERE b.id = :id", Booking.class)
    .getSingleResult();

// Query 2: Initialize passengers separately
em.createQuery(
    "SELECT DISTINCT b FROM Booking b " +
    "LEFT JOIN FETCH b.passengers " +
    "WHERE b.id = :id", Booking.class)
    .getSingleResult();
```

### 2. Database Indexes
```sql
CREATE INDEX idx_booking_reference ON bookings(booking_reference);
CREATE INDEX idx_booking_user_id ON bookings(user_id);
CREATE INDEX idx_payment_booking_id ON payments(booking_id);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_flight_route ON flights(origin_id, destination_id);
CREATE INDEX idx_flight_date ON flights(departure_time);
```

### 3. Caching Strategy
- Flight search results: 5-minute TTL
- Airport/Airline data: 24-hour TTL
- User session: Redis
- JPA second-level cache for entities

## 🧪 Testing

### Test Cards

#### UzCard (Uzbekistan)
```
Card Number: 8600 1234 5678 1234
Expiry: 12/27
CVV: 123
```

#### Humo (Uzbekistan)
```
Card Number: 9860 1234 5678 1234
Expiry: 12/27
CVV: 456
```

#### Visa (International)
```
Card Number: 4111 1111 1111 1111
Expiry: 04/27
CVV: 123
```

#### Test Decline (ends with 0000)
```
Card Number: 4111 1111 1111 0000
Expiry: 04/27
CVV: 123
Status: FAILED
```

### Run Tests
```bash
mvn test
mvn verify
```

## 📊 Key Technical Decisions

### 1. JWT Authentication
- **Why**: Stateless, scalable authentication
- **Implementation**: Custom filter + Spring Security integration
- **Storage**: Access token in memory, refresh token in HTTP-only cookie

### 2. Two-Query Fetching
- **Problem**: Hibernate MultipleBagFetchException
- **Solution**: Split collections into separate queries
- **Benefit**: No Cartesian product, maintains List semantics

### 3. Card Type Detection
- **Why**: Support local and international cards
- **Implementation**: Prefix-based detection with smart validation
- **Cards**: UzCard, Humo (no Luhn), Visa, MasterCard (with Luhn)

### 4. Payment Deduplication
- **Why**: Prevent double charges from duplicate requests
- **Implementation**: Status checking + attempt limiting + cooldown
- **Protection**: COMPLETED check, PROCESSING lock, 5-attempt limit

### 5. Scheduled Expiration
- **Why**: Auto-cleanup unpaid bookings
- **Implementation**: @Scheduled with cron (every 5 minutes)
- **Action**: PENDING → EXPIRED after 15 minutes

## 🔮 Future Enhancements

### Payment Integration
- [ ] **Payme** integration for UzCard/Humo
- [ ] **Click** payment gateway
- [ ] **Stripe** for international cards
- [ ] **PayPal** support
- [ ] **Idempotency keys** for payment deduplication

### Features
- [ ] **Seat selection** with aircraft seat maps
- [ ] **Round-trip bookings** with return flights
- [ ] **Dynamic pricing** based on demand and time
- [ ] **Calendar view** with price comparison
- [ ] **Email notifications** for booking confirmations
- [ ] **SMS notifications** for flight updates
- [ ] **Multi-language** support (UZ, RU, EN)
- [ ] **Loyalty program** with points and rewards

### Technical
- [ ] **Microservices** migration (User, Flight, Booking, Payment)
- [ ] **Event-driven** architecture with Kafka
- [ ] **GraphQL** API for flexible queries
- [ ] **WebSocket** for real-time updates
- [ ] **ElasticSearch** for advanced flight search
- [ ] **Admin dashboard** for management

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'feat: add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

### Commit Message Convention
```
feat: add new feature
fix: bug fix
docs: documentation update
style: code formatting
refactor: code refactoring
test: add tests
chore: maintenance tasks
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Axmadullo Ubaydullayev**
- GitHub: [@Axmadullo2000](https://github.com/Axmadullo2000)
- Email: axmadullo2000@gmail.com

## 🙏 Acknowledgments

- Spring Boot team for excellent framework
- Uzbekistan Airways for ticket design inspiration
- OpenPDF team for PDF generation library

---

⭐ **Star this repository** if you find it helpful!

📚 **Check the Wiki** for detailed documentation

🐛 **Report Issues** on GitHub Issues page