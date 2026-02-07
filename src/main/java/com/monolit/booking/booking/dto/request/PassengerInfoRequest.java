package com.monolit.booking.booking.dto.request;

import com.monolit.booking.booking.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerInfoRequest {

    // ═══════════════════════════════════════
    // СОХРАНЁННЫЙ ПАССАЖИР (ОПЦИОНАЛЬНО)
    // ═══════════════════════════════════════

    // ═══════════════════════════════════════
    // ПЕРСОНАЛЬНЫЕ ДАННЫЕ
    // ═══════════════════════════════════════

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @Pattern(regexp = "^[A-Z]+$", message = "First name must contain only uppercase Latin letters")
    private String firstName;

    @Size(max = 50, message = "Middle name must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z]*$", message = "Middle name must contain only uppercase Latin letters")
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Pattern(regexp = "^[A-Z]+$", message = "Last name must contain only uppercase Latin letters")
    private String lastName;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Nationality is required")
    @Size(min = 2, max = 2, message = "Nationality must be 2-letter ISO code (e.g., RU, US)")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Nationality must be 2 uppercase letters")
    private String nationality;

    // ═══════════════════════════════════════
    // ДОКУМЕНТЫ
    // ═══════════════════════════════════════

    @NotBlank(message = "Passport number is required")
    @Size(min = 5, max = 20, message = "Passport number must be between 5 and 20 characters")
    private String passportNumber;

    @NotBlank(message = "Passport country is required")
    @Size(min = 2, max = 2, message = "Passport country must be 2-letter ISO code")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Passport country must be 2 uppercase letters")
    private String passportCountry;

    @NotNull(message = "Passport expiry date is required")
    @Future(message = "Passport must be valid (expiry date in the future)")
    private LocalDate passportExpiry;

    // ═══════════════════════════════════════
    // КОНТАКТЫ (ОПЦИОНАЛЬНО)
    // ═══════════════════════════════════════

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be 10-15 digits, optionally starting with +")
    private String phoneNumber;

    // ═══════════════════════════════════════
    // ОСОБЫЕ ТРЕБОВАНИЯ
    // ═══════════════════════════════════════

    @Size(max = 500, message = "Special needs must not exceed 500 characters")
    private String specialNeeds;  // wheelchair, vegetarian meal, etc.

    // ═══════════════════════════════════════
    // СОХРАНЕНИЕ
    // ═══════════════════════════════════════

    /**
     * Сохранить пассажира для будущих бронирований?
     */
    private Boolean saveForFuture;
}
