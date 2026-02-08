package com.aviasales.booking.booking.dto.request;

import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.enums.Gender;
import com.aviasales.booking.booking.enums.SeatPreference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Passenger information for booking")
public class PassengerInfoRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String passportNumber;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @NotBlank
    private String nationality;

    @NotNull
    private Gender gender;

    @NotBlank
    private String passportCountry;

    @NotNull
    @Future
    private LocalDate passportExpiry;

    // ✅ ДОБАВЬТЕ КЛАСС КАБИНЫ ДЛЯ КАЖДОГО ПАССАЖИРА
    @Schema(description = "Cabin class for this passenger", example = "BUSINESS")
    private CabinClass cabinClass;  // Если null - используется дефолтный из запроса

    @Pattern(regexp = "^[1-9][0-9]?[A-K]$", message = "Invalid seat format")
    private String seatNumber;

    private SeatPreference seatPreference;

    private Boolean saveForFuture;
}
