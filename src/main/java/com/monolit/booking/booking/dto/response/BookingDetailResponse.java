package com.monolit.booking.booking.dto.response;

import com.monolit.booking.booking.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime confirmedAt;
    private List<BookingFlightResponse> flights;
    private List<PassengerResponse> passengers;
}
