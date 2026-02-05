package com.monolit.booking.booking.mapper;

import com.monolit.booking.booking.dto.response.*;
import com.monolit.booking.booking.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toPaymentResponse(Payment payment);

    PaymentStatusResponse toPaymentStatusResponse(Payment payment);
}
