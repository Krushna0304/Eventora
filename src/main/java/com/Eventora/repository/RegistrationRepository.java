package com.Eventora.repository;

import com.Eventora.dto.EventTemplate;
import com.Eventora.entity.AppUser;
import com.Eventora.entity.Event;
import com.Eventora.entity.Registration;
import com.Eventora.entity.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration,Long> {
    Optional<Registration> findByEventIdAndUser(Long event, AppUser user);
    List<Registration> findByUserAndStatus(AppUser user, RegistrationStatus status);
    List<Registration> findByUser(AppUser user);

    @Query("SELECT e.id FROM Registration e WHERE e.id = :eventId AND e.user = :appUser")
    Optional<Long> checkByIdAndUser(Long eventId,AppUser appUser);

    @Modifying
    @Query("UPDATE Registration r SET r.status = :status WHERE r.event.id = :eventId AND r.user = :user AND r.status <> :status")
    int updateRegistrationStatus(@Param("eventId") Long eventId,
                                 @Param("user") AppUser user,
                                 @Param("status") RegistrationStatus status);
    @Query("SELECT r.status FROM Registration r WHERE r.event.id = :eventId AND r.user = :user")
    Optional<RegistrationStatus> findStatusByEventIdAndUser(@Param("eventId") Long eventId, @Param("user") AppUser user);


    @Modifying
    @Query("UPDATE Registration r SET r.status = :status WHERE r.event.id = :eventId AND r.user = :user AND r.status = :currentStatus")
    int updateRegistrationStatus(@Param("eventId") Long eventId,
                                 @Param("user") AppUser user,
                                 @Param("currentStatus") RegistrationStatus currentStatus,
                                 @Param("status") RegistrationStatus newStatus);

    @Query("""
    SELECT new com.Eventora.dto.EventTemplate(
        e.id,
        e.title,
        e.organizerDisplayName,
        e.eventCategory,
        e.city,
        e.eventStatus,
        e.startDate,
        e.currentParticipants
    )
    FROM Registration r
    JOIN r.event e
    WHERE r.user = :user
      AND r.status = :status
""")
    List<EventTemplate> findRegisteredEventTemplates(@Param("user") AppUser user,
                                                     @Param("status") RegistrationStatus status);



}
