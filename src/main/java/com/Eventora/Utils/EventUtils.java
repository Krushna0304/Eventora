package com.Eventora.Utils;

import com.Eventora.dto.EventDetailDto;
import com.Eventora.dto.EventTemplate;
import com.Eventora.entity.Event;
import com.Eventora.entity.enums.RegistrationStatus;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
public class EventUtils {

    public List<EventTemplate> extractEventTemplates(List<Event> events) {
        return events.stream()
                .map(event -> EventTemplate.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .organizerName(event.getOrganizer() != null ? event.getOrganizer().getUsername() : "Unknown")
                        .eventCategory(event.getEventCategory())
                        .city(event.getCity())
                        .eventStatus(event.getEventStatus())
                        .startDate(event.getStartDate())
                        .participantCount(event.getCurrentParticipants()) // placeholder if you track later
                        .build())
                .toList();
    }
    public EventDetailDto mapToEventDetailDto(Event event,RegistrationStatus userRegisterStatus) {
        if(event == null) return null;

        return EventDetailDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .organizerName(event.getOrganizer() != null ? event.getOrganizer().getUsername() : "Unknown")
                .eventCategory(event.getEventCategory())
                .locationName(event.getLocationName())
                .city(event.getCity())
                .state(event.getState())
                .country(event.getCountry())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .maxParticipants(event.getMaxParticipants())
                .currentParticipants(event.getCurrentParticipants())
                .price(event.getPrice())
                .imageUrl(event.getImageUrl())
                .tags(event.getTags())
                .eventStatus(event.getEventStatus())
                .userRegistrationStatus(userRegisterStatus)
                .build();
    }

}
