🔥 Топ-10 Самых Сложных Частей Проекта
Вот что реально заставило меня страдать (в порядке убывания боли):

1. 💀 Hibernate MultipleBagFetchException
   Сложность: 10/10 | Время: 2 дня
   Проблема:
   java@Entity
   public class Booking {
   @OneToMany(mappedBy = "booking", fetch = FetchType.EAGER)
   private List<Ticket> tickets;  // Collection #1

   @OneToMany(mappedBy = "booking", fetch = FetchType.EAGER)
   private List<Passenger> passengers;  // Collection #2
   }

// Hibernate: "NOPE! Cannot simultaneously fetch multiple bags!"
```

**Ошибка:**
```
org.hibernate.loader.MultipleBagFetchException:
cannot simultaneously fetch multiple bags
Что Я Узнал:

Hibernate не может делать JOIN на две коллекции одновременно
Получается Cartesian product (декартово произведение)
Если 10 билетов и 10 пассажиров = 100 строк вместо 20!

Решение (2-query strategy):
java@Query("SELECT DISTINCT b FROM Booking b " +
"LEFT JOIN FETCH b.tickets " +
"WHERE b.bookingReference = :ref")
Optional<Booking> findWithTickets(@Param("ref") String ref);

@Query("SELECT DISTINCT b FROM Booking b " +
"LEFT JOIN FETCH b.passengers " +  
"WHERE b.bookingReference = :ref")
Optional<Booking> findWithPassengers(@Param("ref") String ref);

// Вызываем обе, инициализируем коллекции
Почему сложно:

Нет очевидного решения
Stack Overflow полон неработающих советов
Каждое "решение" создавало новую проблему


2. 🔐 JWT Authentication + Refresh Token Flow
   Сложность: 9/10 | Время: 1.5 дня
   Проблема:
   java// Где проверять токены?
   // До Spring Security? После? В середине?

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(...) {
        // Проблема 1: Filter вызывается ДО SecurityContext
        // Проблема 2: Нужен SecurityContext для работы
        // Проблема 3: Циклическая зависимость!
    }
}
Что Я Узнал:

OncePerRequestFilter vs GenericFilterBean - разница критична
Порядок фильтров в Spring Security имеет значение
SecurityContextHolder - thread-local, нужно очищать
Refresh token нельзя хранить так же, как access token

Решение:
java@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
return http
.addFilterBefore(
jwtAuthFilter,
UsernamePasswordAuthenticationFilter.class  // ← Важен порядок!
)
.sessionManagement(session ->
session.sessionCreationPolicy(STATELESS)
)
.build();
}
Фишки которые узнал:

Access token: 15 минут, в памяти клиента
Refresh token: 7 дней, HTTP-only cookie
Rotation: каждый refresh выдает новый refresh token
Blacklist: хранить в Redis (не реализовал, но знаю как)


3. 💳 Валидация Карт (UzCard, Humo без Luhn)
   Сложность: 8/10 | Время: 1 день
   Проблема:
   java// Все примеры в интернете:
   public boolean validateCard(String number) {
   return luhnCheck(number);  // ← НЕ РАБОТАЕТ для UzCard/Humo!
   }
   UzCard/Humo не используют алгоритм Luhn!
   Что Я Узнал:

Luhn algorithm - не универсален
UzCard: начинается с 8600, 16 цифр, БЕЗ Luhn
Humo: начинается с 9860, 16 цифр, БЕЗ Luhn
МИР: 2200-2204, с Luhn
UnionPay: 62, иногда с Luhn, иногда нет (!)

Решение:
javapublic CardType detectCardType(String number) {
String prefix = number.substring(0, 4);

    if (number.startsWith("8600")) return CardType.UZCARD;
    if (number.startsWith("9860")) return CardType.HUMO;
    if (number.startsWith("4")) return CardType.VISA;
    if (number.startsWith("5") || 
        (prefix.compareTo("2221") >= 0 && prefix.compareTo("2720") <= 0)) {
        return CardType.MASTERCARD;
    }
    // ... и так далее
}

public boolean validateCard(String number, CardType type) {
// Для UzCard/Humo - только длина и префикс
if (type == CardType.UZCARD || type == CardType.HUMO) {
return number.length() == 16;
}

    // Для международных - Luhn
    return luhnCheck(number);
}
Почему сложно:

Нет единого стандарта
Документация UzCard/Humo на узбекском/русском
Каждая страна - свои правила


4. 🎫 Многоклассовое Бронирование
   Сложность: 8/10 | Время: 1 день
   Проблема:
   java// Изначально: один класс на всё бронирование
   CreateBookingRequest {
   Long flightId;
   CabinClass cabinClass;  // ← ВСЕ пассажиры в одном классе
   List<Passenger> passengers;
   }

// Требование: разные пассажиры - разные классы
// Пример: 2 в Business, 3 в Economy, 1 в First
Что Я Узнал:

Нужна группировка мест по классам: Map<CabinClass, Integer>
Резервирование должно быть атомарным
Цены считаются индивидуально для каждого пассажира
Откат сложно сделать правильно

Решение:
java// Группируем пассажиров по классам
Map<CabinClass, Integer> seatsByClass = new HashMap<>();
for (PassengerInfoRequest passenger : request.getPassengers()) {
CabinClass cls = passenger.getCabinClass() != null
? passenger.getCabinClass()
: defaultClass;
seatsByClass.merge(cls, 1, Integer::sum);
}

// Проверяем доступность ДЛЯ КАЖДОГО КЛАССА
for (Map.Entry<CabinClass, Integer> entry : seatsByClass.entrySet()) {
if (!hasEnoughSeats(flight, entry.getKey(), entry.getValue())) {
throw new InsufficientSeatsException(...);
}
}

// Резервируем места ПО КЛАССАМ
for (Map.Entry<CabinClass, Integer> entry : seatsByClass.entrySet()) {
flight.reserveSeats(entry.getValue(), entry.getKey());
}
Почему сложно:

Транзакции: если один класс full - откатить всё
Динамическое ценообразование на каждого пассажира
Сидения: разные диапазоны для разных классов (1-2 First, 3-8 Business, 9+ Economy)


5. ⏰ Timezone Hell
   Сложность: 7/10 | Время: 3 часа
   Проблема:
   java// Рейс: Ташкент (UTC+5) → Москва (UTC+3)
   // Вылет: 17:00 по Ташкенту
   // Прилёт: 19:20 по Москве
   // Сколько летим? 🤯

// Неправильно:
Duration.between(departureTime, arrivalTime); // 2ч 20мин??? НЕТ!

// Правильно:
Duration.between(departureTimeUTC, arrivalTimeUTC); // 4ч 20мин ✓
Что Я Узнал:

ВСЕГДА храним в UTC: departureTimeUtc, arrivalTimeUtc
Отображаем в локальном времени: departureTime, arrivalTime
Instant для UTC, ZonedDateTime для локального
База данных: TIMESTAMP WITH TIME ZONE в PostgreSQL

Решение:
java@Entity
public class Flight {
@Column(name = "departure_time_utc")
private Instant departureTimeUtc;  // UTC для расчётов

    @Column(name = "departure_time")
    private Instant departureTime;  // Локальное для отображения
    
    // Конверсия при создании
    public void setLocalDepartureTime(ZonedDateTime local) {
        this.departureTimeUtc = local.toInstant();
        this.departureTime = local.toInstant(); // Храним как Instant
    }
}
Фишки:

DST (летнее время) усложняет всё ещё больше
Узбекистан не использует DST с 2016 года (повезло!)
Москва меняет время → проблемы с рейсами


6. 🔁 Payment Deduplication
   Сложность: 7/10 | Время: 4 часа
   Проблема:
   java// Сценарий:
   // 1. Пользователь нажал "Оплатить"
   // 2. Сеть тормозит
   // 3. Пользователь нажал еще раз
   // 4. Две оплаты на одно бронирование! 💸💸
   Что Я Узнал:

Idempotency ключи - must have для платежей
Проверка статуса PROCESSING = lock
Rate limiting по попыткам (5 max)
Cooldown после лимита (30 минут)

Решение:
java@Transactional
public PaymentResponse createPayment(CreatePaymentRequest request) {
// 1️⃣ Idempotency check
if (request.getIdempotencyKey() != null) {
Optional<Payment> existing = paymentRepository
.findByIdempotencyKey(request.getIdempotencyKey());
if (existing.isPresent()) {
return paymentMapper.toResponse(existing.get());  // ← Возвращаем старый
}
}

    // 2️⃣ Проверка существующих платежей
    List<Payment> existing = paymentRepository
        .findByBookingIdAndStatus(bookingId, PaymentStatus.COMPLETED);
    if (!existing.isEmpty()) {
        throw new DuplicatePaymentException("Already paid!");
    }
    
    // 3️⃣ Проверка лимита попыток
    long failedCount = paymentRepository
        .countByBookingIdAndStatusAndCreatedAtAfter(
            bookingId, 
            PaymentStatus.FAILED,
            Instant.now().minus(30, ChronoUnit.MINUTES)
        );
    if (failedCount >= 5) {
        throw new PaymentLimitExceededException("Too many attempts!");
    }
    
    // 4️⃣ Создаём платёж...
}
Проблемы которые решил:

Двойная оплата - предотвращена
Спам попытками - заблокирован
PROCESSING зависшие - очищаются scheduler'ом


7. 📄 PDF Generation с Реалистичным Дизайном
   Сложность: 7/10 | Время: 1 день
   Проблема:
   java// OpenPDF != HTML
   // Нет flexbox, нет grid, нет CSS
   // Всё через таблицы и позиционирование

// Хочу:
┌─────────────────────┐
│  Logo    │  Receipt │
│  AeroStar│  Info    │
└─────────────────────┘

// Реальность:
PdfPTable table = new PdfPTable(2);
table.setWidths(new float[]{1.5f, 1f}); // ???? Подбор значений
Что Я Узнал:

OpenPDF = старая версия iText
Всё строится на PdfPTable
Вложенные таблицы для layout
setWidths() - методом проб и ошибок
Цвета в RGB: new Color(13, 71, 161)

Решение:
java// Карточный дизайн через вложенные таблицы
PdfPTable card = new PdfPTable(1);
card.setWidthPercentage(100);

PdfPCell containerCell = new PdfPCell();
containerCell.setBackgroundColor(new Color(250, 250, 250));
containerCell.setBorderColor(new Color(230, 230, 230));
containerCell.setPadding(15);

// Внутренняя таблица для контента
PdfPTable innerTable = new PdfPTable(2);
innerTable.setWidths(new float[]{1f, 1.5f});
// ... добавляем ячейки

containerCell.addElement(innerTable);
card.addCell(containerCell);
Фишки:

Rectangle.NO_BORDER - убрать границы
setHorizontalAlignment() - выравнивание
setSpacingBefore()/After() - отступы
Эмодзи работают: ✈️ 💳 ✓


8. 🗓️ Scheduled Tasks & Auto-Expiration
   Сложность: 6/10 | Время: 2 часа
   Проблема:
   java// Бронирования истекают через 15 минут
   // Как автоматически отменять?
   // Как освобождать места?
   // Как не нагружать БД?
   Что Я Узнал:

@Scheduled требует @EnableScheduling
Cron expressions: 0 */5 * * * * = каждые 5 минут
Транзакции в scheduler'е - отдельно для каждой записи
Batch операции вместо цикла по одной

Решение:
java@Scheduled(cron = "0 */5 * * * *")  // Каждые 5 минут
@Transactional
public void expireOldBookings() {
LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);

    List<Booking> expired = bookingRepository
        .findByStatusAndExpiresAtBefore(
            BookingStatus.PENDING, 
            cutoff
        );
    
    for (Booking booking : expired) {
        try {
            expireBooking(booking);  // Отдельная транзакция
        } catch (Exception e) {
            log.error("Failed to expire booking {}", booking.getId(), e);
            // Продолжаем с остальными
        }
    }
    
    log.info("Expired {} bookings", expired.size());
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
protected void expireBooking(Booking booking) {
// Освобождаем места
Map<Flight, Map<CabinClass, Long>> seatsToRelease = ...;

    // Отменяем билеты
    booking.setStatus(BookingStatus.EXPIRED);
    
    bookingRepository.save(booking);
}
Проблемы которые решил:

Одна ошибка не ломает весь batch
Места освобождаются корректно
Логирование для мониторинга


9. 🔧 MapStruct + Lombok Configuration
   Сложность: 6/10 | Время: 2 часа
   Проблема:
   xml<!-- Ошибка: MapStruct не видит геттеры/сеттеры Lombok -->
<!-- Compilation error: property "firstName" not found -->

<!-- Почему? Порядок annotation processors! -->
Что Я Узнал:

Lombok генерирует код во время компиляции
MapStruct генерирует код во время компиляции
Если MapStruct идёт первым - геттеров еще нет!

Решение:
xml<plugin>
<groupId>org.apache.maven.plugins</groupId>
<artifactId>maven-compiler-plugin</artifactId>
<version>3.11.0</version>
<configuration>
<source>21</source>
<target>21</target>
<annotationProcessorPaths>
<!-- 1️⃣ ПОРЯДОК ВАЖЕН! Lombok ПЕРВЫМ -->
<path>
<groupId>org.projectlombok</groupId>
<artifactId>lombok</artifactId>
<version>1.18.30</version>
</path>

            <!-- 2️⃣ MapStruct ВТОРЫМ -->
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.5.5.Final</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
Дополнительно:
java@Mapper(
    componentModel = "spring",  // Spring bean
    unmappedTargetPolicy = ReportingPolicy.IGNORE  // Игнорировать лишние поля
)
public interface BookingMapper {
    // MapStruct генерирует реализацию
}

10. 🔗 Foreign Key Cascades & Safe Deletion
    Сложность: 6/10 | Время: 3 часа
    Проблема:
    sql-- Пытаемся удалить рейс:
    DELETE FROM flights WHERE id = 41;

-- PostgreSQL:
ERROR: update or delete on table "flights"
violates foreign key constraint "fk_tickets_flight"
on table "tickets"
Что Я Узнал:

Foreign keys блокируют удаление
CASCADE опасен - удалит всё связанное
Soft delete лучше для production
Нужна валидация перед удалением

Решение:
java@Transactional
public void deleteFlight(Long id) {
Flight flight = flightRepository.findById(id)
.orElseThrow(() -> new FlightNotFoundException(id));

    // ✅ Проверка: есть ли билеты?
    long ticketCount = ticketRepository.countByFlightId(id);
    
    if (ticketCount > 0) {
        throw new FlightDeletionException(
            String.format(
                "Cannot delete flight %s with %d existing tickets. " +
                "Use cancel endpoint instead.",
                flight.getFlightNumber(), 
                ticketCount
            )
        );
    }
    
    // Удаляем только пустые рейсы
    flightRepository.delete(flight);
}

// Альтернатива: мягкое удаление
@Transactional
public FlightDetailResponse cancelFlight(Long id) {
Flight flight = flightRepository.findById(id)
.orElseThrow(() -> new FlightNotFoundException(id));

    flight.setStatus(FlightStatus.CANCELLED);
    flightRepository.save(flight);
    
    return flightMapper.toResponse(flight);
}
Архитектурное решение:

DELETE /api/flights/{id} - только для пустых рейсов
PUT /api/flights/{id}/cancel - для рейсов с билетами
Статус CANCELLED вместо физического удаления


🎓 Главные Уроки
Что Я Освоил:

Hibernate Query Optimization - JOIN FETCH, N+1 queries, query planning
Spring Security Architecture - Filter chains, authentication flow, JWT
Payment System Design - Idempotency, deduplication, rate limiting
Data Consistency - Transactions, isolation levels, optimistic locking
PDF/Document Generation - Layout design without CSS
Timezone Management - UTC storage, local display, DST handling
Scheduled Tasks - Cron expressions, batch processing, error handling
Build Tool Configuration - Annotation processors, plugin ordering
Database Constraints - Foreign keys, cascades, soft deletes
Multi-tenancy Patterns - Class-based grouping, dynamic pricing

Soft Skills:

Stack Overflow Research - находить решения в море мусора
Documentation Reading - Hibernate, Spring, PostgreSQL docs
Debugging Complex Issues - breakpoints, logs, SQL explain
Architecture Trade-offs - monolith vs microservices, когда что использовать


Итог: Это был челлендж, но каждая проблема сделала меня лучшим разработчиком.
Следующий уровень: Microservices 🚀
