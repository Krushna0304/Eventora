package com.Eventora.Utils;

import com.Eventora.dto.EventDetailDto;
import com.Eventora.dto.EventTemplate;
import com.Eventora.entity.Event;
import com.Eventora.entity.enums.EventCategory;
import com.Eventora.entity.enums.EventStatus;
import com.Eventora.entity.enums.RegistrationStatus;
import com.Eventora.projection.EventDetailProjection;
import com.Eventora.projection.EventTemplateProjection;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventUtils {

    public List<EventTemplate> extractEventTemplates(List<Event> events) {
        return events.stream()
                .map(event -> EventTemplate.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .organizerName(event.getOrganizer() != null ? event.getOrganizer().getDisplayName() : "Unknown")
                        .eventCategory(event.getEventCategory())
                        .city(event.getCity())
                        .eventStatus(event.getEventStatus())
                        .startDate(event.getStartDate())
                        .participantCount(event.getCurrentParticipants()) // placeholder if you track later
                        .build())
                .toList();
    }
    public EventDetailDto mapToDto(EventDetailProjection projection) {
        // Parse tags from comma-separated string
        List<String> tags = projection.getTags() != null && !projection.getTags().isEmpty()
                ? Arrays.stream(projection.getTags().split(","))
                .map(String::trim)
                .collect(Collectors.toList())
                : List.of();

        return EventDetailDto.builder()
                .id(projection.getId())
                .title(projection.getTitle())
                .organizerDisplayName(projection.getOrganizerDisplayName())
                .description(projection.getDescription())
                .eventCategory(EventCategory.valueOf(projection.getEventCategory()))
                .locationName(projection.getLocationName())
                .city(projection.getCity())
                .state(projection.getState())
                .country(projection.getCountry())
                .latitude(projection.getLatitude())
                .longitude(projection.getLongitude())
                .startDate(projection.getStartDate())
                .endDate(projection.getEndDate())
                .maxParticipants(projection.getMaxParticipants())
                .currentParticipants(projection.getCurrentParticipants())
                .price(projection.getPrice())
                .imageUrl(projection.getImageUrl())
                .tags(tags)
                .eventStatus(EventStatus.valueOf(projection.getEventStatus()))
                .userRegistrationStatus(RegistrationStatus.valueOf(projection.getUserRegistrationStatus()))
                .build();
    }
    public EventDetailDto mapToEventDetailDto(Event event,RegistrationStatus userRegisterStatus) {
        if(event == null) return null;

        return EventDetailDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .organizerDisplayName(event.getOrganizerDisplayName())
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

    public Page<EventTemplate> mapToEventTemplate(Page<EventTemplateProjection> projections) {
        return projections.map(r ->
                EventTemplate.builder()
                        .id(r.getId())
                        .title(r.getTitle())
                        .organizerName(r.getOrganizerName())
                        .eventStatus(EventStatus.valueOf(r.getEventStatus().toUpperCase()))
                        .city(r.getCity())
                        .eventCategory(EventCategory.valueOf(r.getEventCategory().toUpperCase()))
                        .startDate(r.getStartDate())
                        .participantCount(r.getParticipantCount())
                        .build()
        );
    }

}
