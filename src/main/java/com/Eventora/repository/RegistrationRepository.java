package com.Eventora.repository;

import com.Eventora.entity.AppUser;
import com.Eventora.entity.Event;
import com.Eventora.entity.Registration;
import com.Eventora.entity.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration,Long> {
    Optional<Registration> findByEventAndUser(Event event, AppUser user);
    List<Registration> findByUserAndStatus(AppUser user, RegistrationStatus status);
    List<Registration> findByUser(AppUser user);
}
