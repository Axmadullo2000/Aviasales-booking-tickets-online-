package com.monolit.booking.booking.dto.response;

import com.monolit.booking.booking.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private String bookingReference;
    private BookingStatus status;
    private BigDecimal totalPrice;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
}
