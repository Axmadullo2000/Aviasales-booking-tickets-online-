package com.monolit.booking.booking.repo;

import com.monolit.booking.booking.entity.Booking;
import com.monolit.booking.booking.entity.Users;
import com.monolit.booking.booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ═══════════════════════════════════════
    // ПОИСК ПО BOOKING REFERENCE
    // ═══════════════════════════════════════

    Optional<Booking> findByBookingReference(String bookingReference);

    boolean existsByBookingReference(String bookingReference);

    // ═══════════════════════════════════════
    // ПОИСК ПО ПОЛЬЗОВАТЕЛЮ
    // ═══════════════════════════════════════

    /**
     * Получить все бронирования пользователя с пагинацией
     */
    Page<Booking> findByUser(Users user, Pageable pageable);

    /**
     * Получить бронирования пользователя по статусу
     */
    Page<Booking> findByUserAndStatus(Users user, BookingStatus status, Pageable pageable);

    /**
     * Получить все бронирования пользователя
     */
    List<Booking> findByUser(Users user);

    // ═══════════════════════════════════════
    // ПОИСК ПО СТАТУСУ
    // ═══════════════════════════════════════

    /**
     * Получить бронирования по статусу
     */
    List<Booking> findByStatus(BookingStatus status);

    /**
     * Подсчитать бронирования по статусу
     */
    long countByStatus(BookingStatus status);

    // ═══════════════════════════════════════
    // ПОИСК ИСТЁКШИХ БРОНИРОВАНИЙ
    // ═══════════════════════════════════════

    /**
     * Найти истёкшие бронирования для автоматической отмены
     * Находит все PENDING бронирования с истёкшим сроком оплаты
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'PENDING'
        AND b.expiresAt < :now
    """)
    List<Booking> findExpiredBookings(@Param("now") LocalDateTime now);

    /**
     * Найти бронирования, которые скоро истекут (для напоминаний)
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'PENDING'
        AND b.expiresAt BETWEEN :now AND :threshold
    """)
    List<Booking> findBookingsExpiringBetween(
            @Param("now") LocalDateTime now,
            @Param("threshold") LocalDateTime threshold
    );

    /**
     * Найти бронирование со всеми связанными данными для генерации PDF
     */
    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.tickets t
        LEFT JOIN FETCH t.flight f
        LEFT JOIN FETCH f.airline
        LEFT JOIN FETCH f.origin
        LEFT JOIN FETCH f.destination
        LEFT JOIN FETCH t.passenger
        LEFT JOIN FETCH b.user
        WHERE b.bookingReference = :bookingReference
    """)
    Optional<Booking> findByBookingReferenceWithDetails(
            @Param("bookingReference") String bookingReference
    );

    // ═══════════════════════════════════════
    // СТАТИСТИКА
    // ═══════════════════════════════════════

    /**
     * Подсчитать бронирования пользователя
     */
    long countByUser(Users user);

    /**
     * Получить общую сумму бронирований пользователя
     */
    @Query("""
        SELECT COALESCE(SUM(b.totalAmount), 0)
        FROM Booking b
        WHERE b.user = :user
        AND b.status = 'CONFIRMED'
    """)
    java.math.BigDecimal getTotalAmountByUser(@Param("user") Users user);

    /**
     * Статистика бронирований за период
     */
    @Query("""
        SELECT b.status, COUNT(b), SUM(b.totalAmount)
        FROM Booking b
        WHERE b.createdAt BETWEEN :start AND :end
        GROUP BY b.status
    """)
    List<Object[]> getBookingStatistics(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // ═══════════════════════════════════════
    // ПОИСК С РЕЙСАМИ
    // ═══════════════════════════════════════

    /**
     * Найти бронирования содержащие конкретный рейс
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN b.tickets t
        WHERE t.flight.id = :flightId
    """)
    List<Booking> findBookingsByFlightId(@Param("flightId") Long flightId);

    /**
     * Найти активные бронирования на конкретный рейс
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN b.tickets t
        WHERE t.flight.id = :flightId
        AND b.status IN ('PENDING', 'CONFIRMED')
    """)
    List<Booking> findActiveBookingsByFlightId(@Param("flightId") Long flightId);

    // ═══════════════════════════════════════
    // ПОИСК ПО ДАТАМ
    // ═══════════════════════════════════════

    /**
     * Найти бронирования созданные за период
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.createdAt BETWEEN :start AND :end
    """)
    List<Booking> findBookingsByCreatedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Найти подтверждённые бронирования за период
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.confirmedAt BETWEEN :start AND :end
    """)
    List<Booking> findBookingsByConfirmedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // ═══════════════════════════════════════
    // СПЕЦИАЛЬНЫЕ ЗАПРОСЫ
    // ═══════════════════════════════════════

    /**
     * Получить последние N бронирований пользователя
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.user = :user
        ORDER BY b.createdAt DESC
        LIMIT :limit
    """)
    List<Booking> findLatestBookingsByUser(
            @Param("user") Users user,
            @Param("limit") int limit
    );

    /**
     * Проверить есть ли у пользователя активные бронирования
     */
    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM Booking b
        WHERE b.user = :user
        AND b.status IN ('PENDING', 'CONFIRMED')
    """)
    boolean hasActiveBookings(@Param("user") Users user);

    /**
     * Получить бронирования с предстоящими рейсами
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN b.tickets t
        JOIN t.flight f
        WHERE b.user = :user
        AND b.status = 'CONFIRMED'
        AND f.departureTime > :now
        ORDER BY f.departureTime ASC
    """)
    List<Booking> findUpcomingBookings(
            @Param("user") Users user,
            @Param("now") LocalDateTime now
    );
}
