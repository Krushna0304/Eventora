package com.Eventora.entity;

import com.Eventora.entity.enums.EventCategory;
import com.Eventora.entity.enums.EventStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "events",
        indexes = {
                @Index(name = "idx_event_city", columnList = "city"),
                @Index(name = "idx_event_category", columnList = "eventCategory"),
                @Index(name = "idx_event_status", columnList = "eventStatus"),
                @Index(name = "idx_event_location", columnList = "latitude, longitude")
        }

)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventCategory eventCategory;


    @Column(nullable = false)
    private String locationName;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, columnDefinition = "double precision")
    private Double latitude;

    @Column(nullable = false, columnDefinition = "double precision")
    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Integer maxParticipants;

    private Integer currentParticipants = 0;

    @Column(precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.valueOf(0.00d);

    private String imageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "event_tags",
            joinColumns = @JoinColumn(name = "event_id")
    )
    @Column(name = "tag", length = 50)
    private List<String> tags = new ArrayList<>();


    @CreationTimestamp
    @Column(nullable = true)
    private LocalDateTime postedAt;

    @Min(0)
    @Column(nullable = true)
    private Integer postedDaysBeforeEvent;

    @Column(nullable = true)
    private Long promotionSpend = 0L;

    @Column(nullable = true, columnDefinition = "double precision")
    private Double organizerReputation = 0.0; // 0.0 - 1.0

    @Column(nullable = true, columnDefinition = "double precision")
    private Double avgPastAttendanceRate = 0.0; // 0.0 - 1.0

    @Column(nullable = true, length = 20)
    private String cityCategory; // small, medium, large

    @Column(nullable = false)
    private Integer impressions = 0;

    @Column(nullable = false)
    private Integer clicks = 0;

    @Column(nullable = true, columnDefinition = "double precision")
    private Double ctr = 0.0;

    @Column(nullable = true)
    private Integer socialMentions = 0;

    @Column(nullable = true)
    private Integer tagsCount = 0;

    @Column(nullable = true)
    private Integer checkedInCount = 0;

    @Column(precision = 12, scale = 2)
    private BigDecimal revenue = BigDecimal.valueOf(0.00d); // checkedInCount * price

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventStatus eventStatus = EventStatus.UPCOMING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private AppUser organizer;

    @Column(nullable = false)
    private String organizerDisplayName;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void beforeSaveOrUpdate() {
        // recalculate postedDaysBeforeEvent
        if (startDate != null && postedAt != null) {
            this.postedDaysBeforeEvent = (int) ChronoUnit.DAYS.between(postedAt, startDate);
        }

        // update tags count
        this.tagsCount = (tags == null) ? 0 : tags.size();

        // update CTR
        if (impressions != null && impressions > 0) {
            this.ctr = (double) clicks / impressions;
        } else {
            this.ctr = 0.0;
        }

        // calculate revenue
        if (price != null && checkedInCount != null) {
            this.revenue = price.multiply(BigDecimal.valueOf(checkedInCount));
        } else {
            this.revenue = BigDecimal.ZERO;
        }

        if(organizer.getDisplayName() != null){
            this.organizerDisplayName = organizer.getDisplayName();
        }
    }


}
