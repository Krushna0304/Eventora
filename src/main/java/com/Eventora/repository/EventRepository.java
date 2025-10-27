package com.Eventora.repository;

import com.Eventora.entity.AppUser;
import com.Eventora.entity.Event;
import com.Eventora.entity.enums.EventCategory;
import com.Eventora.entity.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> , JpaSpecificationExecutor<Event> {
    List<Event> findByEventCategory(EventCategory category);
    List<Event> findByEventStatus(EventStatus status);
    List<Event> findByCityIgnoreCase(String city);
    List<Event> findByCityIgnoreCaseAndEventCategory(String city, EventCategory category);
    List<Event> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    List<Event> findByIdAndOrganizer(Long eventId,AppUser appUser);
    List<Event> findByOrganizerAndEndDateBefore(AppUser organizer, LocalDateTime dateTime);

    @Query("SELECT e FROM Event e JOIN e.tags t WHERE LOWER(t) = LOWER(:tag)")
    List<Event> findByTag(@Param("tag") String tag);

    @Query("SELECT DISTINCT e FROM Event e JOIN e.tags t WHERE LOWER(t) IN :tags")
    List<Event> findByTagsIn(@Param("tags") List<String> tags);

    @Query("SELECT e FROM Event e WHERE e.startDate > CURRENT_TIMESTAMP")
    List<Event> findUpcomingEvents();

    List<Event> findByStartDateBetween(LocalDateTime start, LocalDateTime end);
    List<Event> findByCityIgnoreCaseAndEventCategoryAndEventStatus(
            String city, EventCategory category, EventStatus status);
}
