package com.Eventora.dto;

import com.Eventora.entity.enums.EventCategory;
import com.Eventora.entity.enums.EventStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTemplate {
    private Long id;
    private String title;
    private String organizerName;
    private EventCategory eventCategory;
    private String city;
    private EventStatus eventStatus;
    private LocalDateTime startDate;
    private Integer participantCount;
    private Boolean isLiked;
}
