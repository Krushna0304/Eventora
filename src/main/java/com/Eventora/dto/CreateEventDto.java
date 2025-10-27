package com.Eventora.dto;

import com.Eventora.entity.enums.EventCategory;
import com.Eventora.entity.enums.EventStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


public record CreateEventDto(
        String title,
        String description,
        EventCategory eventCategory,
        String locationName,
        String city,
        String state,
        String country,

        //retrive from frontend
        Double latitude,
        Double longitude,

        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer maxParticipants,
        BigDecimal price,

        // Optional field for image URL - can be null Acept as a multipart file store in aws s3 bucket and store the url in db
        String imageUrl,
        List<String> tags,
        EventStatus eventStatus,
        Long promotionSpend,
        Integer socialMentions
) {}
