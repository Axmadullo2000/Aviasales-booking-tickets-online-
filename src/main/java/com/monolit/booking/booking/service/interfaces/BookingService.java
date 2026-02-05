package com.monolit.booking.booking.service.interfaces;

import com.monolit.booking.booking.dto.request.*;
import com.monolit.booking.booking.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request, Long userId);

    BookingDetailResponse getBookingByReference(String bookingReference, Long userId);

    Page<BookingResponse> getUserBookings(Long userId, Pageable pageable);

    BookingResponse confirmBooking(String bookingReference, Long userId);

    BookingResponse cancelBooking(String bookingReference, Long userId);

    byte[] generateTicketPdf(String bookingReference, Long userId);
}
