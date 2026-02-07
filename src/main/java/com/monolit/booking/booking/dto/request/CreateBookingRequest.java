package com.monolit.booking.booking.dto.request;

import com.monolit.booking.booking.enums.CabinClass;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    // ═══════════════════════════════════════
    // РЕЙС
    // ═══════════════════════════════════════

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    @NotNull(message = "Cabin class is required")
    private CabinClass cabinClass;  // ECONOMY, BUSINESS, FIRST_CLASS

    // ═══════════════════════════════════════
    // ПАССАЖИРЫ
    // ═══════════════════════════════════════

    @NotNull(message = "Passengers list is required")
    @Size(min = 1, max = 9, message = "Must have between 1 and 9 passengers")
    @Valid  // валидация вложенных объектов
    private List<PassengerInfoRequest> passengers;

    // ═══════════════════════════════════════
    // КОНТАКТНАЯ ИНФОРМАЦИЯ
    // ═══════════════════════════════════════

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String contactEmail;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must be 10-15 digits, optionally starting with +")
    private String contactPhone;

    // ═══════════════════════════════════════
    // ДОПОЛНИТЕЛЬНАЯ ИНФОРМАЦИЯ
    // ═══════════════════════════════════════

    @Size(max = 1000, message = "Special requests must not exceed 1000 characters")
    private String specialRequests;  // особые требования для всего бронирования

}
