package com.monolit.booking.booking.dto.request;


import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CancelBookingRequest {
    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
    private String reason;
}
