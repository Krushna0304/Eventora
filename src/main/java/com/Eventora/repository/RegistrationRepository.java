package com.Eventora.repository;

import com.Eventora.entity.AppUser;
import com.Eventora.entity.Event;
import com.Eventora.entity.Registration;
import com.Eventora.entity.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration,Long> {
    Optional<Registration> findByEventAndUser(Event event, AppUser user);
    List<Registration> findByUserAndStatus(AppUser user, RegistrationStatus status);
}
