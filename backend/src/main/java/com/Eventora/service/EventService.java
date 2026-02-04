package com.Eventora.service;
import com.Eventora.Utils.ApplicationContextUtils;
import com.Eventora.Utils.EventUtils;
import com.Eventora.dto.*;
import com.Eventora.entity.AppUser;
import com.Eventora.entity.Event;
import com.Eventora.entity.enums.EventCategory;
import com.Eventora.entity.enums.EventStatus;
import com.Eventora.entity.enums.InteractionType;
import com.Eventora.entity.enums.RegistrationStatus;
import com.Eventora.projection.EventDetailProjection;
import com.Eventora.projection.EventTemplateProjection;
import com.Eventora.repository.AppUserRepository;
import com.Eventora.repository.EventRepository;
import com.Eventora.security.CustomUserDetails;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final RegistrationService registrationService;
    private final AWSS3Service awss3Service;
    private final EventRepository eventRepository;
    private final AppUserRepository appUserRepository;
    private final EventUtils eventUtils;
    private final ApplicationContextUtils applicationContextUtils;
    private final UserInteractionService userInteractionService;
    private final ApplicationEventPublisher publisher;

    //Done
    public EventDetailDto createEvent(CreateEventDto createEventDto, MultipartFile file) throws Exception {
        if (applicationContextUtils.getLoggedUser() == null)
            throw new RuntimeException("No logged user found");

        AppUser organizer = applicationContextUtils.getLoggedUser();

        // Build Event entity from DTO
        String fileUrl = file == null ? createEventDto.imageUrl(): awss3Service.uploadFile(file);
        Event event = Event.builder()
                .title(createEventDto.title())
                .description(createEventDto.description())
                .eventCategory(createEventDto.eventCategory())
                .locationName(createEventDto.locationName())
                .city(createEventDto.city())
                .state(createEventDto.state())
                .country(createEventDto.country())
                .latitude(createEventDto.latitude())
                .longitude(createEventDto.longitude())
                .startDate(createEventDto.startDate())
                .endDate(createEventDto.endDate())
                .maxParticipants(createEventDto.maxParticipants())
                .price(createEventDto.price() != null ? createEventDto.price() : BigDecimal.ZERO)
                .imageUrl(fileUrl)
                .tags(createEventDto.tags())
                .eventStatus(createEventDto.eventStatus() != null ? createEventDto.eventStatus() : EventStatus.UPCOMING)
                .organizer(organizer)
                .currentParticipants(0)
                .impressions(0)
                .checkedInCount(0)
                .clicks(0)
                .promotionSpend(createEventDto.promotionSpend() != null ? createEventDto.promotionSpend() : Integer.valueOf(0))
                .socialMentions(createEventDto.socialMentions() != null ? createEventDto.socialMentions() : 0)
                .cityCategory(classifyCity(createEventDto.city()))
                .avgPastAttendanceRate(calcuateAvgPastAttendanceRate(organizer))
                .organizerReputation(calcuateAvgPastAttendanceRate(organizer))
                .build();

        // Save to repository
        eventRepository.save(event);
        return eventUtils.mapToEventDetailDto(event, RegistrationStatus.NONE);
    }

    public String classifyCity(String cityName) {
        // Simple classification based on city name length (for demonstration purposes)
        if (cityName == null || cityName.isEmpty()) {
            return "Unknown";
        }
        int length = cityName.length();
        if (length <= 4) {
            return "Small";
        } else if (length <= 7) {
            return "Medium";
        } else {
            return "Large";
        }
    }
    //CREATE TABLE for this
    public Double calcuateAvgPastAttendanceRate(AppUser organizer) {
        return 0.0;
//
//        List<Event> pastEvents = eventRepository.findByOrganizerAndEndDateBefore(organizer,java.time.LocalDateTime.now());
//        if(pastEvents.isEmpty())
//            return 0.0;
//        double totalAttendees =0.0;
//        double maxParticipants=0.0;
//        for(Event event: pastEvents)
//        {
//            if(event.getMaxParticipants()==0)
//                continue;
//            totalAttendees += (double)event.getCheckedInCount();
//            maxParticipants += (double)event.getMaxParticipants();
//
//        }
//        return totalAttendees/maxParticipants;
    }

    public List<EventTemplate> getFilteredEvents(EventFilterRequest filter) {

        List<Event> events = eventRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Price Range (BigDecimal)
            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }

            // City, State, Country (String)
            if (filter.getCity() != null && !filter.getCity().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), filter.getCity().toLowerCase()));
            }
            if (filter.getState() != null && !filter.getState().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("state")), filter.getState().toLowerCase()));
            }
            if (filter.getCountry() != null && !filter.getCountry().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("country")), filter.getCountry().toLowerCase()));
            }

            // Event Category (Enum)
            if (filter.getEventCategory() != null) {
                predicates.add(cb.equal(root.get("eventCategory"), filter.getEventCategory()));
            }

            // Nearby radius filter (latitude & longitude)
            if (filter.getLatitude() != null && filter.getLongitude() != null && filter.getRadiusInKm() != null) {
                double lat = filter.getLatitude();
                double lon = filter.getLongitude();
                double radius = filter.getRadiusInKm();

                // Approximate latitude/longitude bounds
                double latMin = lat - radius / 111.0;
                double latMax = lat + radius / 111.0;
                double lonMin = lon - radius / (111.0 * Math.cos(Math.toRadians(lat)));
                double lonMax = lon + radius / (111.0 * Math.cos(Math.toRadians(lat)));

                predicates.add(cb.between(root.get("latitude"), latMin, latMax));
                predicates.add(cb.between(root.get("longitude"), lonMin, lonMax));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        });

        for(Event event: events) {
            event.setImpressions(event.getImpressions()+1);

            if(applicationContextUtils.getLoggedUser() != null){

            publisher.publishEvent(new UserInteractionEvent(
                    applicationContextUtils.getLoggedUser().getId(),
                    event.getId(),
                    InteractionType.VIEW_EVENT,
                    Map.of()
            ));
            }
        }
        eventRepository.saveAll(events);
        // Convert to EventTemplate DTOs
        List<Long> likedEventIds = new ArrayList<>();
        if(applicationContextUtils.getLoggedUser() != null){

            likedEventIds = userInteractionService.getLikedEventIds();
        }
        return eventUtils.extractEventTemplates(events,likedEventIds);
    }

    //Done
    @Transactional
    public Page<EventTemplate> findEventByNameAndOrganizer
    (String eventName,String organizerName,Boolean isMyList,int page,int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());

        // Normalize empty strings to null
        eventName = (eventName != null && eventName.trim().isEmpty()) ? null : eventName;
        organizerName = (organizerName != null && organizerName.trim().isEmpty()) ? null : organizerName;

        boolean fetchMyList = Boolean.TRUE.equals(isMyList);

        if (fetchMyList) {
            Long userId = applicationContextUtils.getLoggedUser().getId();
            return eventRepository.searchMyRegisteredEvents(userId, eventName, organizerName, pageable);
        } else {
            List<Long> likedEventIds = userInteractionService.getLikedEventIds();
            return eventUtils.mapToEventTemplate(eventRepository.searchEvents(eventName, organizerName, pageable),likedEventIds);
        }
    }

    //done
    @Transactional
    public EventDetailDto getEventById(Long eventId) {
        eventRepository.updateClicks(eventId);
        RegistrationStatus userRegistrationStatus = RegistrationStatus.NONE;
        if (applicationContextUtils.getLoggedUser() != null) {
            AppUser appUser = applicationContextUtils.getLoggedUser();
            userRegistrationStatus = registrationService.checkIsUserRegisteredForEvent(eventId, appUser);
        }
        EventDetailProjection projection = eventRepository.findEventDetailById(
                eventId,
                userRegistrationStatus.name()
        ).orElseThrow(() -> new RuntimeException("Event not found"));

        publisher.publishEvent(new UserInteractionEvent(
                applicationContextUtils.getLoggedUser().getId(),
                eventId,
                InteractionType.VIEW_EVENT,
                Map.of()
        ));

        return eventUtils.mapToDto(projection);
    }

    //done
    @Transactional
    public void cancelEvent(Long eventId) throws Exception {
        if (applicationContextUtils.getLoggedUser() == null)
            throw new RuntimeException("No logged user found");

        AppUser appUser = applicationContextUtils.getLoggedUser();

        Optional<Long> isPresent = eventRepository.checkByIdAndOrganizer(eventId,appUser);
        if(!isPresent.isPresent()) {
            throw new Exception("Event not found or you are not the organizer");
        }
        eventRepository.setEventStatus(eventId,EventStatus.CANCELLED);
    }

    //done
    public void scheduleEvent(Long eventId) throws Exception {
        if (applicationContextUtils.getLoggedUser() == null)
            throw new RuntimeException("No logged user found");

        AppUser appUser = applicationContextUtils.getLoggedUser();

        Optional<Long> isPresent = eventRepository.checkByIdAndOrganizer(eventId,appUser);
        if(!isPresent.isPresent()) {
            throw new Exception("Event not found or you are not the organizer");
        }

        eventRepository.setEventStatus(eventId,EventStatus.SCHEDULED);
    }

    //done
    public EventDetailDto modifyEvent(Long eventId, CreateEventDto updatedEventDto, MultipartFile file) throws Exception {
        if (applicationContextUtils.getLoggedUser() == null)
            throw new RuntimeException("No logged user found");

        AppUser organizer = applicationContextUtils.getLoggedUser();

        List<Event> events = eventRepository.findByIdAndOrganizer(eventId,organizer);
        if(events.isEmpty()) {
            throw new Exception("Event not found or you are not the organizer");
        }
        // Build Event entity from DTO
        String fileUrl = file == null ? updatedEventDto.imageUrl(): awss3Service.uploadFile(file);
        Event event = events.get(0);
                 event.setTitle(updatedEventDto.title())
                ;event.setDescription(updatedEventDto.description())
                ;event.setEventCategory(updatedEventDto.eventCategory())
                ;event.setLocationName(updatedEventDto.locationName())
                ;event.setCity(updatedEventDto.city())
                ;event.setState(updatedEventDto.state())
                ;event.setCountry(updatedEventDto.country())
                ;event.setLatitude(updatedEventDto.latitude())
                ;event.setLongitude(updatedEventDto.longitude())
                ;event.setStartDate(updatedEventDto.startDate())
                ;event.setEndDate(updatedEventDto.endDate())
                ;event.setMaxParticipants(updatedEventDto.maxParticipants())
                ;event.setPrice(updatedEventDto.price() != null ? updatedEventDto.price() : BigDecimal.ZERO)
                ;event.setImageUrl(fileUrl)
                ;event.setTags(updatedEventDto.tags())
                ;event.setEventStatus(updatedEventDto.eventStatus() != null ? updatedEventDto.eventStatus() : EventStatus.UPCOMING)
                ;event.setOrganizer(organizer)
                ;event.setCurrentParticipants(0)
                ;
            event.setPromotionSpend(updatedEventDto.promotionSpend() != null ? updatedEventDto.promotionSpend() : 0);
            event.setSocialMentions(updatedEventDto.socialMentions() != null ? updatedEventDto.socialMentions() : 0);

        // Save to repository
        eventRepository.save(event);
        return eventUtils.mapToEventDetailDto(event, RegistrationStatus.NONE);
    }
    //Done
    public Page<EventTemplate> getEventByNameOrganiserByMe(String eventTitle, int page,int size) throws Exception {
        AppUser organizer = applicationContextUtils.getLoggedUser();
        if (organizer == null) {
            throw new Exception("Organizer not found");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
        List<Long> likedEventIds = userInteractionService.getLikedEventIds();
        return eventUtils.mapToEventTemplate(eventRepository.searchEventsOrganizeByMe(eventTitle, organizer.getId(), pageable),likedEventIds);
    }


    //no need to reatrive the user,event forcefully when retriving registration - done
    //
    //apply pagination findEventByNameAndOrganizer,getFilteredEvents,getEventByNameOrganiserByMe
    //add index on id,organizer on event table for faster lookup - 3 lookups
    //no need to reatrive the organizer forcefully when retriving event - only name required

}
