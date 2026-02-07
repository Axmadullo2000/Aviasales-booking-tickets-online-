package com.monolit.booking.booking.embedded;


import lombok.*;

@Builder
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class ContactInfo {
    private String email;
    private String phone;
    private String alternativePhone;
    private String contactPerson;
}
