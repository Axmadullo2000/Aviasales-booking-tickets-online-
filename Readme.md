# ✈️ Aviasales - The Brutally Honest README

A **monolithic flight booking system** that actually works (most of the time). Built by one developer who learned Spring Boot, fought Hibernate, and survived to tell the tale.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue.svg)](https://www.postgresql.org/)
[![Status](https://img.shields.io/badge/Status-Monolith%20Complete-blue.svg)]()
[![Next](https://img.shields.io/badge/Next-Microservices-purple.svg)]()

## 🎯 What This Actually Is

This is a **learning project** that turned into a fully functional flight booking backend. No, it's not running any real flights (yet). Yes, the payment gateway is mocked. But everything else? It works.

**Current Status**: Monolith is complete, tested, and stable. Next phase: splitting into microservices and building a proper frontend.

---

## ✅ What Actually Works

### Things I'm Proud Of:

#### ✅ JWT Authentication
- Token refresh works flawlessly
- Role-based authorization isn't just decorative
- Actually prevents unauthorized access (tested by accident many times)

#### ✅ Flight Search
- Multi-criteria search that doesn't explode with N+1 queries
- Fought Hibernate for 2 days to avoid `MultipleBagFetchException`
- Implemented two-query strategy - not elegant, but rock solid

#### ✅ Booking System
- Handles 9 passengers per booking (tested with realistic Uzbek names)
- Mixed cabin classes in single booking (this was surprisingly hard)
- Auto-expiration scheduler that actually runs
- Cancellation with proper refund calculation based on time until departure

#### ✅ Payment Processing
- Supports **UzCard** and **Humo** (because I'm from Uzbekistan 🇺🇿)
- Also Visa, MasterCard, МИР, AmEx, UnionPay
- Validates cards properly (Luhn algorithm for international, custom for local)
- Prevents duplicate payments (learned this the hard way)
- Failed attempt tracking with cooldown (max 5 tries, then 30-min timeout)

#### ✅ PDF Generation
- Generates actual boarding passes that look professional
- Styled for **AeroStar Airlines** (made-up name, real design)
- Barcode included (doesn't actually work at airports, obviously)
- Three languages: English, Russian, Uzbek

#### ✅ Error Handling
- Custom exceptions for everything
- GlobalExceptionHandler that actually catches errors
- Meaningful error messages (not just "Internal Server Error")

---

## 🚫 What Doesn't Work (Yet)

### Let's Be Honest:

#### ❌ Payment Gateway Integration

```java
// This is basically the entire "payment processing"
private boolean processPaymentMock(CreatePaymentRequest request) {
    String cardNumber = request.getCardNumber();
    // If card ends with 0000 → fail, otherwise → success
    return !cardNumber.endsWith("0000");
}
```

Yeah, it's mocked. Payme/Click integration is planned for microservices phase.

#### ❌ Email Notifications

```java
// TODO: Actually send emails
log.info("Email would be sent to: {}", booking.getContactEmail());
```

The infrastructure is there. The SMTP config is not.

#### ❌ Seat Selection UI
- Backend can handle seat selection
- You can request "12A" and it works
- But there's no visual seat map
- Frontend will fix this

#### ❌ Real-time Updates
- No WebSockets yet
- Booking status? You gotta refresh
- Flight delays? Check manually
- This will be fixed with microservices + message queue

#### ❌ Flight Data Population
- Airports and airlines are manually inserted
- No automated flight schedule imports
- Admin has to create flights via Swagger
- Real airlines use GDS systems - we don't (yet)

#### ❌ File Storage
- PDFs are generated on-the-fly
- Not stored anywhere permanently
- No S3/MinIO integration
- Each download regenerates the PDF (wasteful, I know)

---

## 🤔 Questionable Decisions (That I Made Anyway)

### 1. **Two-Query Fetching Strategy**

```java
// Query 1: Get booking + tickets
// Query 2: Get booking + passengers
// Why? Because Hibernate throws MultipleBagFetchException
```

**Why I did it**: Spent 6 hours fighting Hibernate. This works. Moving on.

**Better solution**: GraphQL, or just accept Sets instead of Lists. But Lists are nicer for ordering.

### 2. **Storing Prices in Multiple Places**

- `Flight` has `basePrice`, `businessPrice`, `firstClassPrice`
- `Ticket` has `price`, `baseFare`, `taxes`
- `Payment` has `amount`

**Why**: Started simple, got complex, never refactored.

**Should fix**: Yes. Will I? Probably in microservices rewrite.

### 3. **Dynamic Pricing That's Too Aggressive**

```java
if (daysUntilDeparture <= 1) {
    return new BigDecimal("2.0");  // 2x price!
}
```

**Reality**: This is insane. Airlines don't double prices overnight.

**Fix**: Made it gentler (1.3x max). Still not realistic, but better.

### 4. **15-Minute Booking Expiration**

```java
private static final int EXPIRATION_MINUTES = 15;
```

**Why so short**: Testing. Didn't want to wait hours.

**Real airlines**: 24 hours minimum. Will adjust for production.

### 5. **Scheduled Task Every 5 Minutes**

```java
@Scheduled(cron = "0 */5 * * * *")
public void expireOldBookings() { ... }
```

**Why**: Seemed reasonable for demo.

**Production**: Should be event-driven, not polling. Kafka will handle this in microservices.

---

## 💀 The Painful Parts

### Things That Took Forever:

**1. Hibernate MultipleBagFetchException** (2 days)
- Multiple `@OneToMany` with `FetchType.EAGER` = death
- Solutions tried: 15+
- Solution that worked: Don't fetch multiple bags at once
- Lesson learned: Hibernate hates me

**2. JWT Filter Order** (4 hours)
- Filter runs before Spring Security
- But needs Spring Security to work
- Circular dependency hell
- Fixed with: `.addFilterBefore()`

**3. Card Validation** (1 day)
- UzCard/Humo don't use Luhn algorithm
- Every example online assumes Luhn
- Had to write custom validation
- Now supports both

**4. Timezone Handling** (3 hours)

```java
// Moscow is UTC+3, Tashkent is UTC+5
// Storing in UTC, displaying in local
// Convert, convert, convert...
```

Still not 100% sure it's right in all cases.

**5. MapStruct Configuration** (2 hours)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>21</source>
        <target>21</target>
        <annotationProcessorPaths>
            <!-- This order matters! -->
```

Order matters. Lombok first, then MapStruct. Don't ask why.

---

## 🏗️ Architecture (The Good and The Bad)

### Current State: **Monolith**

```
┌──────────────────────────────────────┐
│                                      │
│         EVERYTHING IN ONE JAR        │
│                                      │
│  Auth │ Flights │ Bookings │ Payments│
│                                      │
│         PostgreSQL Database          │
│                                      │
└──────────────────────────────────────┘
```

**Pros:**
- Simple deployment
- Easy to debug
- Fast development
- Works perfectly for current scale

**Cons:**
- One crash = everything down
- Can't scale individual services
- Changing booking logic requires full redeploy
- Database is single point of failure

### Future State: **Microservices** (Planned)

```
┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐
│  Auth   │  │ Flight  │  │ Booking │  │ Payment │
│ Service │  │ Service │  │ Service │  │ Service │
└────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘
     │            │            │            │
     └────────────┴────────────┴────────────┘
                  │
            ┌─────▼─────┐
            │   Kafka   │
            └───────────┘
```

**Why split:**
- Payment service can be updated independently
- Booking service can scale during peak times
- Flight search can have its own cache layer
- Failures are isolated

**When:** Next phase of development.

---

## 📊 Performance (Real Numbers)

### What I Actually Tested:

**Search Flights**: ~150ms (10 airports, 50 flights)
- Acceptable for now
- Will need ElasticSearch for 1000+ flights

**Create Booking**: ~300ms (9 passengers, mixed classes)
- Includes validation, seat assignment, price calculation
- Not bad for a monolith

**Generate PDF**: ~500ms
- OpenPDF isn't the fastest
- Should cache or use async generation

**Database Queries**:
- N+1 queries: **Fixed** (used JOIN FETCH)
- Cartesian product: **Fixed** (two-query strategy)
- Missing indexes: **Fixed** (added 8 indexes)

---

## 🧪 Testing Reality

### Unit Tests:
- **Coverage**: ~60%
- **Status**: Most critical paths covered
- **Reality**: Not as many as I wanted, but core logic is tested

### Integration Tests:
- **Status**: Manual testing via Swagger
- **Automation**: Planned for microservices phase
- **Reality**: Postman collection exists, not automated

### Load Tests:
- **Status**: Haven't done them
- **Why**: It's a demo, not production
- **Should I**: Yes, eventually

---

## 🚀 Getting Started (Actually Works)

### Prerequisites That Actually Matter:

- **Java 21** - Not 17, not 11. Java 21. (Uses virtual threads, latest syntax)
- **PostgreSQL 15+** - MySQL won't work, don't try
- **Maven 3.8+** - Gradle? Not configured for this

### Setup (5 minutes, seriously):

```bash
# 1. Clone
git clone https://github.com/Axmadullo2000/Aviasales-booking-tickets-online-.git
cd Aviasales-booking-tickets-online-

# 2. Database
docker-compose up -d postgres

# 3. Configure (application.yml)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/booking_db
    username: postgres
    password: postgres  # Change this!

# 4. Run
mvn spring-boot:run

# 5. Test
curl http://localhost:8080/actuator/health
```

**Swagger UI**: http://localhost:8080/swagger-ui.html

**Default Admin**:
```
Email: axmadullo2000@gmail.com
Password: admin123
```
(Change this before deploying anywhere!)

---

## 🎯 What's Next

### Roadmap (Actually Realistic):

**Phase 1: Microservices** (Current)
- [ ] Split into 4 services (Auth, Flight, Booking, Payment)
- [ ] Add Kafka for event-driven architecture
- [ ] Implement API Gateway
- [ ] Add service discovery (Eureka/Consul)
- [ ] Containerize everything (Docker)

**Phase 2: Frontend** (After Microservices)
- [ ] React/Next.js UI
- [ ] Seat selection map
- [ ] Real-time booking updates
- [ ] Admin dashboard
- [ ] Mobile responsive

**Phase 3: Real Integrations**
- [ ] Payme/Click payment gateway
- [ ] Email service (SendGrid/AWS SES)
- [ ] SMS notifications
- [ ] GDS integration for real flight data

**Phase 4: Production Features**
- [ ] Kubernetes deployment
- [ ] Monitoring (Prometheus + Grafana)
- [ ] Distributed tracing (Jaeger)
- [ ] Load balancing
- [ ] CI/CD pipeline

---

## 🤷 Known Issues

### Bugs I Know About:

1. **Receipt `flight_details` can overflow** - Fixed by changing to TEXT
2. **Dynamic pricing too aggressive** - Made gentler, still not realistic
3. **Timezone edge cases** - Works for Tashkent/Moscow, may break elsewhere
4. **PDF generation is synchronous** - Blocks request thread
5. **No rate limiting** - Can spam the API freely

### Won't Fix (In Monolith):

- Real-time updates → WebSockets in microservices
- Distributed transactions → Saga pattern in microservices
- Service mesh → Istio with Kubernetes
- Horizontal scaling → When we actually need it

---

## 💭 Lessons Learned

### Technical:

1. **Hibernate is both amazing and terrible** - JOIN FETCH everything, or suffer
2. **JWT is simple until it isn't** - Refresh tokens, token rotation, blacklisting
3. **Card validation is surprisingly complex** - Every country is different
4. **Timezones are hard** - UTC for storage, local for display, always
5. **PDF generation needs caching** - Or async, or both

### Personal:

1. **Don't over-engineer** - Monolith first, then split
2. **Mock when needed** - Real payment gateway isn't essential for MVP
3. **Test core paths** - 100% coverage is a myth
4. **Documentation matters** - Even for solo projects
5. **Realistic data helps** - Uzbek names made testing feel real

---

## 🤝 Contributing

### Can You Help?

**Yes! Here's how:**

**Code:**
- Bug fixes welcome
- Performance improvements appreciated
- Don't refactor everything at once

**Ideas:**
- UX improvements
- Feature suggestions
- Architecture feedback

**No Thanks:**
- "Why didn't you use [framework X]?" - Too late
- "This should be in [language Y]!" - It's Java, deal with it
- "You should rewrite everything" - I know

### How to Contribute:

```bash
git checkout -b feature/your-awesome-feature
git commit -m "feat: actually describe what you did"
git push origin feature/your-awesome-feature
```

Then create a PR with:
- What you changed
- Why you changed it
- How you tested it

---

## 📝 License

MIT - Do whatever you want. Copy it, sell it, make it better. Just don't blame me if it breaks.

---

## 🙏 Special Thanks

- **Spring Boot Team** - For making Java development not suck
- **Hibernate Team** - For the pain that taught me SQL
- **Stack Overflow** - For solving every error I encountered
- **ChatGPT/Claude** - For explaining Kafka when I was too tired to read docs
- **Coffee** - For existing

---

## 👨‍💻 Author

**Axmadullo Ubaydullayev**
- GitHub: [@Axmadullo2000](https://github.com/Axmadullo2000)
- Email: axmadullo2000@gmail.com
- Location: Uzbekistan 🇺🇿
- Status: Currently refactoring everything into microservices

---

## 📌 Final Thoughts

This project started as a learning exercise and became a fully functional booking system. It's not perfect. The code has quirks. Some decisions were compromises. But it **works**, it's **documented**, and it's **ready** for the next phase.

**Next up**: Splitting this monolith into microservices and building a frontend that actually looks good.

---

⭐ **Star if you like honest READMEs**

🐛 **Issues welcome** (I probably know about it already)

💬 **Questions?** Open an issue, I'll answer

---

*Last updated: February 2026*  
*Status: Monolith Complete ✅ | Microservices In Progress 🚧 | Frontend Planned 📱*
