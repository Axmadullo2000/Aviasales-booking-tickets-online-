package com.aviasales.booking.booking.controller;

import com.aviasales.booking.booking.dto.request.CancelBookingRequest;
import com.aviasales.booking.booking.dto.request.CreateBookingRequest;
import com.aviasales.booking.booking.dto.response.BookingDetailResponse;
import com.aviasales.booking.booking.dto.response.BookingResponse;
import com.aviasales.booking.booking.exception.InsufficientSeatsException;
import com.aviasales.booking.booking.projection.UsersProjection;
import com.aviasales.booking.booking.service.interfaces.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;


@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management endpoints")
public class BookingController {
    private final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Create a new booking", description = "Create a new flight booking")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UsersProjection userDetails
    ) throws InsufficientSeatsException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, userDetails.getId()));
    }

    @GetMapping("/{reference}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get booking by reference", description = "Get detailed booking information by booking reference")
    public ResponseEntity<BookingDetailResponse> getBookingByReference(
            @Parameter(description = "Booking reference (6 characters)")
            @PathVariable String reference,
            @AuthenticationPrincipal UsersProjection userDetails
    ) {
        return ResponseEntity.ok(bookingService.getBookingByReference(reference, userDetails.getId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get user bookings", description = "Get all bookings for the authenticated user")
    public ResponseEntity<Page<BookingResponse>> getUserBookings(
            @AuthenticationPrincipal UsersProjection userDetails,
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = userDetails.getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(bookingService.getUserBookings(userId, pageable));
    }

    @PostMapping("/{reference}/confirm")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Confirm a booking", description = "Confirm a pending booking")
    public ResponseEntity<BookingResponse> confirmBooking(
            @Parameter(description = "Booking reference")
            @PathVariable String reference,
            @AuthenticationPrincipal UsersProjection userDetails
    ) {
        return ResponseEntity.ok(bookingService.confirmBooking(reference, userDetails.getId()));
    }

    @DeleteMapping("/{reference}/cancel")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Cancel booking", description = "Cancel a booking by reference")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to cancel this booking"),
            @ApiResponse(responseCode = "400", description = "Booking cannot be cancelled")
    })
    public ResponseEntity<BookingResponse> cancelBooking(
            @Parameter(description = "Booking reference (e.g., ABC123)")
            @PathVariable String reference,

            @Parameter(description = "Cancellation details (optional)")
            @Valid @RequestBody(required = false) CancelBookingRequest request,

            @AuthenticationPrincipal UsersProjection userDetails
    ) {
        log.info("Cancel booking request: {}, user: {}", reference, userDetails.getUsername());

        try {
            String reason = request != null ? request.getReason() : null;

            BookingResponse response = bookingService.cancelBooking(
                    reference,
                    userDetails.getId(),
                    reason
            );

            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            log.warn("Access denied for user {} trying to cancel booking {}",
                    userDetails.getUsername(), reference);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Not authorized to cancel this booking",
                    e
            );
        }
    }

    @GetMapping("/{reference}/ticket")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Download ticket PDF", description = "Download the e-ticket as PDF")
    public ResponseEntity<byte[]> downloadTicket(
            @Parameter(description = "Booking reference")
            @PathVariable String reference,
            @AuthenticationPrincipal UsersProjection userDetails
    ) {
        byte[] pdfContent = bookingService.generateTicketPdf(reference, userDetails.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "ticket-" + reference + ".pdf");
        headers.setContentLength(pdfContent.length);

        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }
}
