package com.aviasales.booking.booking.entity;

import com.aviasales.booking.booking.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "passengers")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ПЕРСОНАЛЬНЫЕ ДАННЫЕ
    @Column(nullable = false)
    private String firstName;

    private String middleName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private PassengerType passengerType;  // ADULT, CHILD, INFANT

    @Column(nullable = false, length = 2)
    private String nationality;

    // ДОКУМЕНТЫ (КРИТИЧЕСКИ ВАЖНО!)

    @Column(nullable = false)
    private String passportNumber;

    @Column(nullable = false, length = 2)
    private String passportCountry; // RU, US, AE

    @Column(nullable = false)
    private LocalDate passportExpiry;

    // КОНТАКТЫ

    private String email;
    private String phoneNumber;

    // Связь с пользователем

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user; // NULL если одноразовый пассажир

    private Boolean isSaved;  // сохранить для будущих бронирований

    // ОСОБЫЕ ТРЕБОВАНИЯ

    @Column(columnDefinition = "TEXT")
    private String specialNeeds;  // wheelchair, meal preferences, etc.

    // АУДИТ

    @CreatedDate
    private LocalDateTime createdAt;

    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public PassengerType determinePassengerType() {
        int age = getAge();

        if (age < 2) return PassengerType.INFANT;
        if (age < 12) return PassengerType.CHILD;

        return PassengerType.ADULT;
    }

    public boolean isPassportValid(LocalDate travelDate) {
        return passportExpiry.isAfter(travelDate.plusMonths(6));
    }

}
