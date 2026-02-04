package com.monolit.booking.booking.projection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface UsersProjection extends UserDetails {
    Long getId();
    String getEmail();

    @Value("#{target.role.![role.name]}")
    List<String> getRoleNames();

    @Override
    default String getUsername() {
        return getEmail();
    }

    @Override
    default Collection<? extends GrantedAuthority> getAuthorities() {
        return getRoleNames().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

}
