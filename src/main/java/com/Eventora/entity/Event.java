package com.Eventora.entity;

import com.Eventora.entity.enums.EventCategory;
import com.Eventora.entity.enums.EventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Builder
@Data
@NoArgsConstructor // Add this
@AllArgsConstructor // Optional, if you want an all-args constructor
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

    @Column(precision = 9, nullable = false)
    private Double latitude;

    @Column(precision = 9, nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Integer maxParticipants;

   // @Column(nullable = false)
    private Integer currentParticipants = 0;

    @Column(precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.valueOf(0.00d);

    private String imageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "event_tags",
            joinColumns = @JoinColumn(name = "event_id")
    )
    @Column(name = "tag", nullable = false, length = 50)
    private List<String> tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventStatus eventStatus = EventStatus.UPCOMING;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organizer_id", nullable = false)
    private AppUser organizer;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
