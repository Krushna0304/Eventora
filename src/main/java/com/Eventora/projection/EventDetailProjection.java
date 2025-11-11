package com.Eventora.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface EventDetailProjection {
    Long getId();
    String getTitle();
    String getOrganizerDisplayName();
    String getDescription();
    String getEventCategory();
    String getLocationName();
    String getCity();
    String getState();
    String getCountry();
    Double getLatitude();
    Double getLongitude();
    LocalDateTime getStartDate();
    LocalDateTime getEndDate();
    Integer getMaxParticipants();
    Integer getCurrentParticipants();
    BigDecimal getPrice();
    String getImageUrl();
    String getEventStatus();
    String getTags();
    String getUserRegistrationStatus();
}