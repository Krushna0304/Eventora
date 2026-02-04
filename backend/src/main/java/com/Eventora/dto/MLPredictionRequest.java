package com.Eventora.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLPredictionRequest {

    @JsonProperty("event_id")
    private Long eventId;

    @JsonProperty("tags_count")
    private Integer tagsCount;

    @JsonProperty("posted_days_before_event")
    private Integer postedDaysBeforeEvent;

    @JsonProperty("promotion_spend")
    private Long promotionSpend;

    @JsonProperty("max_participants")
    private Integer maxParticipants;

    @JsonProperty("ticket_price")
    private Double ticketPrice;

    @JsonProperty("organizer_reputation")
    private Double organizerReputation;

    @JsonProperty("avg_past_attendance_rate")
    private Double avgPastAttendanceRate;

    @JsonProperty("ctr")
    private Double ctr;

    @JsonProperty("social_mentions")
    private Integer socialMentions;

    @JsonProperty("weekday")
    private Integer weekday;

    @JsonProperty("category")
    private String category;

    @JsonProperty("city")
    private String city;
}
