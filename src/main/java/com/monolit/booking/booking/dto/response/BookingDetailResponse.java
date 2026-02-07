package com.monolit.booking.booking.dto.response;

import com.monolit.booking.booking.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailResponse {

    private Long id;
    private String bookingReference;
    private BookingStatus status;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;
    private List<BookingFlightResponse> flights;
    private List<PassengerResponse> passengers;
}
