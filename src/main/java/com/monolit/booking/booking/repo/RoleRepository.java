package com.monolit.booking.booking.repo;

import com.monolit.booking.booking.entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Roles, Long> {

}
