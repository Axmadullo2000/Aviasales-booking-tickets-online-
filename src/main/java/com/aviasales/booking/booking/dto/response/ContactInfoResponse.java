package com.aviasales.booking.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Contact information")
public class ContactInfoResponse {

    @Schema(description = "Contact email", example = "user@example.com")
    private String email;

    @Schema(description = "Contact phone", example = "+998901234567")
    private String phone;
}
