package com.Eventora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_analytics", indexes = {
        @Index(name = "idx_analytics_event", columnList = "event_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    private LocalDateTime snapshotAt; // when the analytics snapshot was taken

    private Integer tagsCount;
    private Integer postedDaysBeforeEvent;
    private Long promotionSpend;
    private Integer maxParticipants;
    @Column(precision = 10, scale = 2)
    private BigDecimal ticketPrice;

    @Column(columnDefinition = "double precision")
    private Double organizerReputation;

    @Column(columnDefinition = "double precision")
    private Double avgPastAttendanceRate;

    @Column(columnDefinition = "double precision")
    private Double ctr;
    private Integer socialMentions;
    private Integer weekday;
    private String cityCategory;
    private Integer checkedInCount;
    @Column(precision = 12, scale = 2)
    private BigDecimal revenue;
    private Integer success; // 0/1 label

    // optional: extra JSON / metadata column
    @Column(columnDefinition = "TEXT")
    private String metadata;
}
