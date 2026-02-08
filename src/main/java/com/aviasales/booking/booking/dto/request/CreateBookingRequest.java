package com.aviasales.booking.booking.dto.request;

import com.aviasales.booking.booking.enums.CabinClass;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotNull
    private Long flightId;

    // ✅ ТЕПЕРЬ ОПЦИОНАЛЬНЫЙ - используется как дефолт
    @Schema(description = "Default cabin class (can be overridden per passenger)",
            example = "ECONOMY")
    private CabinClass defaultCabinClass;

    @NotEmpty
    @Size(min = 1, max = 9)
    private List<PassengerInfoRequest> passengers;

    @NotBlank
    @Email
    private String contactEmail;

    @NotBlank
    private String contactPhone;

    private String specialRequests;
}
