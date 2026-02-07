package com.monolit.booking.booking.service.interfaces;

import com.monolit.booking.booking.dto.request.CreateBookingRequest;
import com.monolit.booking.booking.dto.response.BookingDetailResponse;
import com.monolit.booking.booking.dto.response.BookingResponse;
import com.monolit.booking.booking.exception.InsufficientSeatsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface BookingService {

    /**
     * Создать новое бронирование
     */
    BookingResponse createBooking(CreateBookingRequest request, Long userId) throws InsufficientSeatsException;

    /**
     * Получить детальную информацию о бронировании
     */
    BookingDetailResponse getBookingByReference(String bookingReference, Long userId);

    /**
     * Получить все бронирования пользователя с пагинацией
     */
    Page<BookingResponse> getUserBookings(Long userId, Pageable pageable);

    /**
     * Подтвердить бронирование (после оплаты)
     */
    BookingResponse confirmBooking(String bookingReference, Long userId);

    /**
     * Отменить бронирование
     */
    BookingResponse cancelBooking(String bookingReference, Long userId, String reason);  // ✅ добавил reason

    /**
     * Сгенерировать PDF билета
     */
    byte[] generateTicketPdf(String bookingReference, Long userId);
}
