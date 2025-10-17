package com.Eventora.dto;

import com.Eventora.entity.enums.EventCategory;
import com.Eventora.entity.enums.EventStatus;
import com.Eventora.entity.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDetailDto {
    private Long id;
    private String title;
    private String organizerName;
    private String description;
    private EventCategory eventCategory;
    private String locationName;
    private String city;
    private String state;
    private String country;
    private Double latitude;
    private Double longitude;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private BigDecimal price;
    private String imageUrl;
    private List<String> tags;
    private EventStatus eventStatus;
    private RegistrationStatus userRegistrationStatus; // New field to indicate user's registration status
}
