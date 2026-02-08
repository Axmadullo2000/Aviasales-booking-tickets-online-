package com.aviasales.booking.booking.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Roles> role;
    private String firstName;
    private String lastName;

    public Users(String email, String password, List<Roles> role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }
}
