package com.Eventora.repository;

import com.Eventora.dto.EventDetailDto;
import com.Eventora.dto.EventTemplate;
import com.Eventora.entity.AppUser;
import com.Eventora.entity.Event;
import com.Eventora.entity.enums.EventCategory;
import com.Eventora.entity.enums.EventStatus;
import com.Eventora.entity.enums.RegistrationStatus;
import com.Eventora.projection.EventDetailProjection;
import com.Eventora.projection.EventTemplateProjection;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> , JpaSpecificationExecutor<Event> {
    List<Event> findByEventCategory(EventCategory category);
    List<Event> findByEventStatus(EventStatus status);
    List<Event> findByCityIgnoreCase(String city);
    List<Event> findByCityIgnoreCaseAndEventCategory(String city, EventCategory category);
    List<Event> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    List<Event> findByIdAndOrganizer(Long eventId,AppUser appUser);
    List<Event> findByOrganizerAndEndDateBefore(AppUser organizer, LocalDateTime dateTime);
    List<Event> findByOrganizer(AppUser organizer);
    @Query("SELECT e FROM Event e JOIN e.tags t WHERE LOWER(t) = LOWER(:tag)")
    List<Event> findByTag(@Param("tag") String tag);

    @Query("SELECT DISTINCT e FROM Event e JOIN e.tags t WHERE LOWER(t) IN :tags")
    List<Event> findByTagsIn(@Param("tags") List<String> tags);

    @Query("SELECT e FROM Event e WHERE e.startDate > CURRENT_TIMESTAMP")
    List<Event> findUpcomingEvents();

    List<Event> findByStartDateBetween(LocalDateTime start, LocalDateTime end);
    List<Event> findByCityIgnoreCaseAndEventCategoryAndEventStatus(
            String city, EventCategory category, EventStatus status);

    @Query("""
    SELECT e FROM Event e
    WHERE 
      (:eventName IS NULL OR :eventName = '' 
          OR LOWER(e.title) LIKE LOWER(CONCAT('%', :eventName, '%')))
    AND 
      (:organizerName IS NULL OR :organizerName = '' 
          OR LOWER(e.organizerDisplayName) LIKE LOWER(CONCAT('%', :organizerName, '%')))
    """)
    List<Event> searchEvents(@Param("eventName") String eventName,
                             @Param("organizerName") String organizerName);


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
    FROM Event e
    """)
    List<EventTemplate> findAllEventTemplates();


    @Modifying
    @Query(
            """
                UPDATE Event e
                SET e.currentParticipants = e.currentParticipants + :val
                WHERE e.id = :eventId
            """
    )
    void updateParticipantCount(@Param("eventId") Long eventId, @Param("val") int val);

    @Query(value = """
                SELECT 
                    e.id,
                    e.title,
                    e.organizer_display_name,
                    e.description,
                    e.event_category,
                    e.location_name,
                    e.city,
                    e.state,
                    e.country,
                    e.latitude,
                    e.longitude,
                    e.start_date,
                    e.end_date,
                    e.max_participants,
                    e.current_participants,
                    e.price,
                    e.image_url,
                    e.event_status,
                    COALESCE(
                        (SELECT string_agg(et.tag, ',') 
                         FROM event_tags et 
                         WHERE et.event_id = e.id),
                        ''
                    ) as tags,
                    CAST(:userRegistrationStatus AS VARCHAR) as user_registration_status
                FROM events e
                WHERE e.id = :eventId
                """, nativeQuery = true)
    Optional<EventDetailProjection> findEventDetailById(
            @Param("eventId") Long eventId,
            @Param("userRegistrationStatus") String userRegistrationStatus
    );

    @Modifying
    @Transactional
    @Query("""
    UPDATE Event e
    SET e.clicks = e.clicks + 1
    WHERE e.id = :eventId
    """)
    void updateClicks(@Param("eventId") Long eventId);

    @Modifying
    @Transactional
    @Query("""
    UPDATE Event e
    SET e.checkedInCount = e.checkedInCount + 1
    WHERE e.id = :eventId
    """)
    void increaseCheckedInCnt(@Param("eventId") Long eventId);


    @Modifying
    @Transactional
    @Query("""
    UPDATE Event e
    SET e.eventStatus = :status
    WHERE e.id = :eventId
    """)
    void setEventStatus(@Param("eventId") Long eventId, @Param("status") EventStatus status);


    @Query("SELECT e.id FROM Event e WHERE e.id = :eventId AND e.organizer = :appUser")
    Optional<Long> checkByIdAndOrganizer(Long eventId,AppUser appUser);


    @Modifying
    @Query("""
        UPDATE Event e
        SET e.currentParticipants = e.currentParticipants + 1
        WHERE e.id = :eventId
          AND e.currentParticipants < e.maxParticipants
          AND e.eventStatus = com.Eventora.entity.enums.EventStatus.SCHEDULED
    """)
    int incrementParticipantCountIfAvailable(@Param("eventId") Long eventId);

    @Query("SELECT e.id FROM Event e WHERE e.id = :eventId")
    Optional<Long> isEventPresent(Long eventId);


    // Search all events with projection (no full entity fetch)
    @Query(value = """
    SELECT 
        e.id,
        e.title,
        e.organizer_display_name as organizerName,
        e.event_category as eventCategory,
        e.city,
        e.event_status as eventStatus,
        e.start_date as startDate,
        e.current_participants as participantCount
    FROM events e
    WHERE 
        (:eventTitle IS NULL OR :eventTitle = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :eventTitle, '%')))
        AND (:organizerName IS NULL OR :organizerName = '' OR LOWER(e.organizer_display_name) LIKE LOWER(CONCAT('%', :organizerName, '%')))
    ORDER BY e.start_date DESC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM events e
    WHERE 
        (:eventTitle IS NULL OR :eventTitle = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :eventTitle, '%')))
        AND (:organizerName IS NULL OR :organizerName = '' OR LOWER(e.organizer_display_name) LIKE LOWER(CONCAT('%', :organizerName, '%')))
    """,
            nativeQuery = true)
    Page<EventTemplateProjection> searchEvents(
            @Param("eventTitle") String eventTitle,
            @Param("organizerName") String organizerName,
            Pageable pageable
    );

    // Search user's registered events
    @Query(value = """
    SELECT 
        e.id,
        e.title,
        e.organizer_display_name as organizerName,
        e.event_category as eventCategory,
        e.city,
        e.event_status as eventStatus,
        e.start_date as startDate,
        e.current_participants as participantCount
    FROM events e
    INNER JOIN event_registrations er ON er.event_id = e.id
    WHERE 
        er.user_id = :userId
        AND er.status != 'CANCELLED'
        AND (:eventTitle IS NULL OR :eventTitle = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :eventTitle, '%')))
        AND (:organizerName IS NULL OR :organizerName = '' OR LOWER(e.organizer_display_name) LIKE LOWER(CONCAT('%', :organizerName, '%')))
    ORDER BY e.start_date DESC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM events e
    INNER JOIN event_registrations er ON er.event_id = e.id
    WHERE 
        er.user_id = :userId
        AND er.status != 'CANCELLED'
        AND (:eventTitle IS NULL OR :eventTitle = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :eventTitle, '%')))
        AND (:organizerName IS NULL OR :organizerName = '' OR LOWER(e.organizer_display_name) LIKE LOWER(CONCAT('%', :organizerName, '%')))
    """,
            nativeQuery = true)
    Page<EventTemplate> searchMyRegisteredEvents(
            @Param("userId") Long userId,
            @Param("eventTitle") String eventTitle,
            @Param("organizerName") String organizerName,
            Pageable pageable
    );

    @Query(value = """
    SELECT 
        e.id,
        e.title,
        e.organizer_display_name AS organizerName,
        e.event_category AS eventCategory,
        e.city,
        e.event_status AS eventStatus,
        e.start_date AS startDate,
        e.current_participants AS participantCount
    FROM events e
    WHERE 
        (:eventTitle IS NULL OR :eventTitle = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :eventTitle, '%')))
        AND e.organizer_id = :organizerId
    ORDER BY e.start_date DESC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM events e
    WHERE 
        (:eventTitle IS NULL OR :eventTitle = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :eventTitle, '%')))
        AND e.organizer_id = :organizerId
    """,
            nativeQuery = true)
    Page<EventTemplateProjection> searchEventsOrganizeByMe(
            @Param("eventTitle") String eventTitle,
            @Param("organizerId") Long organizerId,
            Pageable pageable
    );
}



