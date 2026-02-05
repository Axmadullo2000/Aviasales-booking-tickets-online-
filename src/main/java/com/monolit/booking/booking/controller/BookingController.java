package com.monolit.booking.booking.controller;

import com.monolit.booking.booking.dto.request.CreateBookingRequest;
import com.monolit.booking.booking.dto.response.BookingDetailResponse;
import com.monolit.booking.booking.dto.response.BookingResponse;
import com.monolit.booking.booking.entity.Users;
import com.monolit.booking.booking.projection.UsersProjection;
import com.monolit.booking.booking.service.interfaces.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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


@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management endpoints")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Create a new booking", description = "Create a new flight booking")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UsersProjection userDetails
    ) {
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

    @PostMapping("/{reference}/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Cancel a booking", description = "Cancel an existing booking")
    public ResponseEntity<BookingResponse> cancelBooking(
            @Parameter(description = "Booking reference")
            @PathVariable String reference,
            @AuthenticationPrincipal UsersProjection userDetails
    ) {
        return ResponseEntity.ok(bookingService.cancelBooking(reference, userDetails.getId()));
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
