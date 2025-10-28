package com.Eventora.service;
import com.Eventora.Utils.ApplicationContextUtils;
import com.Eventora.Utils.EventUtils;
import com.Eventora.dto.CreateEventDto;
import com.Eventora.dto.EventDetailDto;
import com.Eventora.dto.EventFilterRequest;
import com.Eventora.dto.EventTemplate;
import com.Eventora.entity.AppUser;
import com.Eventora.entity.Event;
import com.Eventora.entity.enums.EventStatus;
import com.Eventora.entity.enums.RegistrationStatus;
import com.Eventora.repository.AppUserRepository;
import com.Eventora.repository.EventRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private final RestAPIService restAPIService;

    public EventDetailDto createEvent(CreateEventDto createEventDto, MultipartFile file) throws Exception {
        String email = applicationContextUtils.getLoggedUserEmail();
        AppUser organizer = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("Logged-in user not found"));

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
                .cityCategory(restAPIService.classifyCity(createEventDto.city()))
                .avgPastAttendanceRate(calcuateAvgPastAttendanceRate())
                .organizerReputation(calcuateAvgPastAttendanceRate())
                .build();

        // Save to repository
        eventRepository.save(event);
        return eventUtils.mapToEventDetailDto(event, RegistrationStatus.NONE);
    }

    public Double calcuateAvgPastAttendanceRate() {
        String email = applicationContextUtils.getLoggedUserEmail();
        AppUser organizer = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
        List<Event> pastEvents = eventRepository.findByOrganizerAndEndDateBefore(organizer,java.time.LocalDateTime.now());
        if(pastEvents.isEmpty())
            return 0.0;
        double totalAttendees =0.0;
        double maxParticipants=0.0;
        for(Event event: pastEvents)
        {
            if(event.getMaxParticipants()==0)
                continue;
            totalAttendees += (double)event.getCheckedInCount();
            maxParticipants += (double)event.getMaxParticipants();

        }
        return totalAttendees/maxParticipants;
    }

    public List<EventTemplate> findEventByNameAndOrganizer(String eventName, String organizerName,Boolean isMyList) throws Exception {
        boolean fetchMyList = Boolean.TRUE.equals(isMyList);
        if(fetchMyList){
        List<EventTemplate> myEvents = registrationService.getRegisteredEvents();
            myEvents = myEvents.stream().filter(e->
                    (eventName ==null || eventName.isEmpty() ||
                            e.getTitle().toLowerCase().contains(eventName.toLowerCase())) &&
                            (organizerName == null || organizerName.isEmpty() ||
                                    e.getOrganizerName().toLowerCase().contains(organizerName.toLowerCase())))
                    .collect(Collectors.toList());
            return myEvents;
        }
        List<Event> events = eventRepository.searchEvents(eventName, organizerName);
        return eventUtils.extractEventTemplates(events);
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
        }
        eventRepository.saveAll(events);
        // Convert to EventTemplate DTOs
        return eventUtils.extractEventTemplates(events);
    }

    public EventDetailDto getEventById(Long eventId) throws Exception {
        Event event =eventRepository.findById(eventId)
                .orElseThrow(() -> new Exception("Event not found with id: " + eventId));
        String email = applicationContextUtils.getLoggedUserEmail();
        RegistrationStatus userStatus = RegistrationStatus.NONE;
        if(email.equals("anonymousUser")==false)
        {
            AppUser appUser = appUserRepository.findByEmail(email)
                    .orElseThrow(() -> new Exception("User not found"));
            userStatus = registrationService.checkIsUserRegisteredForEvent(event,appUser);
        }
        event.setClicks(event.getClicks()+1);
        return eventUtils.mapToEventDetailDto(event,userStatus);
    }

    public void cancelEvent(Long eventId) throws Exception {
        AppUser appUser = appUserRepository.findByEmail(applicationContextUtils.getLoggedUserEmail())
                .orElseThrow(() -> new Exception("Logged-in user not found"));
        List<Event> event = eventRepository.findByIdAndOrganizer(eventId,appUser);
        if(event.isEmpty()) {
            throw new Exception("Event not found or you are not the organizer");
        }
        event.get(0).setEventStatus(EventStatus.CANCELLED);
        eventRepository.saveAll(event);
    }

    public void scheduleEvent(Long eventId) throws Exception {
        AppUser appUser = appUserRepository.findByEmail(applicationContextUtils.getLoggedUserEmail())
                .orElseThrow(() -> new Exception("Logged-in user not found"));
        List<Event> event = eventRepository.findByIdAndOrganizer(eventId,appUser);
        if(event.isEmpty()) {
            throw new Exception("Event not found or you are not the organizer");
        }
        event.get(0).setEventStatus(EventStatus.SCHEDULED);
        eventRepository.saveAll(event);
    }

    public EventDetailDto modifyEvent(Long eventId, CreateEventDto updatedEventDto, MultipartFile file) throws Exception {
        String email = applicationContextUtils.getLoggedUserEmail();
        AppUser organizer = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("Logged-in user not found"));
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

    public List<EventTemplate> getEventByNameOrganiserByMe(String eventName) throws Exception {
        AppUser organizer = appUserRepository.findByEmail(applicationContextUtils.getLoggedUserEmail())
                .orElseThrow(() -> new Exception("Organizer not found"));
        List<Event> organizerEvents = eventRepository.findByOrganizer(organizer);
        List <EventTemplate> myEvents =  eventUtils.extractEventTemplates(organizerEvents);
        List<EventTemplate> filterdEvents = myEvents;
        if(eventName!=null && !eventName.isEmpty()){
            filterdEvents = myEvents.stream().filter(e->
                    (eventName ==null || eventName.isEmpty() ||
                            e.getTitle().toLowerCase().contains(eventName.toLowerCase())))
                    .collect(Collectors.toList());;
        }
        return filterdEvents;
    }

}
