package com.Eventora.dto;

import com.Eventora.entity.enums.EventCategory;
import com.Eventora.entity.enums.EventStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreateEventDto(

        @NotBlank(message = "Event title is required.")
        String title,

        @NotBlank(message = "Event description is required.")
        String description,

        @NotNull(message = "Event category must be specified.")
        EventCategory eventCategory,

        @NotBlank(message = "Location name is required.")
        String locationName,

        @NotBlank(message = "City is required.")
        String city,

        @NotBlank(message = "State is required.")
        String state,

        @NotBlank(message = "Country is required.")
        String country,

        @NotNull(message = "Latitude is required.")
        Double latitude,

        @NotNull(message = "Longitude is required.")
        Double longitude,

        @NotNull(message = "Start date is required.")
        LocalDateTime startDate,

        @NotNull(message = "End date is required.")
        LocalDateTime endDate,

        @NotNull(message = "Maximum participants must be provided.")
        @Positive(message = "Maximum participants must be greater than 0.")
        Integer maxParticipants,

        @NotNull(message = "Event price must be provided.")
        @PositiveOrZero(message = "Event price must be zero or greater.")
        BigDecimal price,

        @NotBlank(message = "Image URL is required after uploading to S3.")
        String imageUrl,

        @NotEmpty(message = "At least one tag must be provided.")
        List<String> tags,

        @NotNull(message = "Event status must be specified.")
        EventStatus eventStatus,

        @NotNull(message = "Promotion spend is required.")
        @PositiveOrZero(message = "Promotion spend must be zero or greater.")
        Long promotionSpend,

        @NotNull(message = "Social mentions count is required.")
        @PositiveOrZero(message = "Social mentions must be zero or greater.")
        Integer socialMentions

) {}
