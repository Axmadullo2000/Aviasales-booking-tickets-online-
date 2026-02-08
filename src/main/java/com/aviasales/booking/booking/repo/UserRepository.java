package com.aviasales.booking.booking.repo;

import com.aviasales.booking.booking.entity.Users;
import com.aviasales.booking.booking.projection.UsersProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    UsersProjection findByEmail(String email);

    Optional<Users> findUsersById(Long id);
}
