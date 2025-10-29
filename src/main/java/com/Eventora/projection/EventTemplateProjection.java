package com.Eventora.projection;


import java.time.LocalDateTime;

public interface EventTemplateProjection {
        Long getId();
        String getTitle();
        String getOrganizerName();
        String getEventCategory();
        String getCity();
        String getEventStatus();
        LocalDateTime getStartDate();
        Integer getParticipantCount();
}


