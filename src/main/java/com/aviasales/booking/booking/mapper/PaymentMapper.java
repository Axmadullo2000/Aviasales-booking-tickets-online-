package com.aviasales.booking.booking.mapper;

import com.aviasales.booking.booking.dto.response.PaymentResponse;
import com.aviasales.booking.booking.dto.response.PaymentStatusResponse;
import com.aviasales.booking.booking.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentStatusResponse toPaymentStatusResponse(Payment payment);

    @Mapping(target = "changeAmount", ignore = true)  // Устанавливается в сервисе
    @Mapping(target = "message", ignore = true)       // Устанавливается в сервисе
    PaymentResponse toPaymentResponse(Payment payment);
}
