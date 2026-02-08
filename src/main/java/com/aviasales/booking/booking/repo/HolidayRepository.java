package com.aviasales.booking.booking.repo;

import com.aviasales.booking.booking.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository для работы с праздниками
 */
@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    /**
     * Найти праздник по дате и стране
     */
    Optional<Holiday> findByHolidayDateAndCountryAndIsActiveTrue(LocalDate date, String country);

    /**
     * Найти все активные праздники страны
     */
    List<Holiday> findByCountryAndIsActiveTrueOrderByHolidayDate(String country);

    /**
     * Найти праздники в диапазоне дат
     */
    @Query("SELECT h FROM Holiday h WHERE h.holidayDate BETWEEN :startDate AND :endDate " +
            "AND h.country = :country AND h.isActive = true")
    List<Holiday> findHolidaysInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("country") String country
    );

    /**
     * Найти праздники которые могут влиять на дату
     * (с учётом daysRange)
     */
    @Query("SELECT h FROM Holiday h WHERE h.isActive = true " +
            "AND h.country = :country " +
            "AND h.holidayDate BETWEEN :startDate AND :endDate")
    List<Holiday> findHolidaysAffectingDate(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("country") String country
    );

    /**
     * Проверить есть ли праздник на дату
     */
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END " +
            "FROM Holiday h WHERE h.holidayDate = :date " +
            "AND h.country = :country AND h.isActive = true")
    boolean isHoliday(@Param("date") LocalDate date, @Param("country") String country);
}
