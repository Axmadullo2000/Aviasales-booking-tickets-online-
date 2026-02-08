package com.aviasales.booking.booking.service.interfaces;

import com.aviasales.booking.booking.dto.request.CreateBookingRequest;
import com.aviasales.booking.booking.dto.response.AvailableSeatsResponse;
import com.aviasales.booking.booking.dto.response.BookingDetailResponse;
import com.aviasales.booking.booking.dto.response.BookingResponse;
import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.exception.InsufficientSeatsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.nio.file.AccessDeniedException;


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
    BookingResponse cancelBooking(String reference, Long userId, String reason) throws AccessDeniedException;

    /**
     * Сгенерировать PDF билета
     */
    byte[] generateTicketPdf(String bookingReference, Long userId);

    AvailableSeatsResponse getAvailableSeats(Long flightId, CabinClass cabinClass);

}
