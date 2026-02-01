package com.Eventora.entity;

import com.Eventora.entity.enums.EventCategory;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_interactions")
public class UserInteraction {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long userId;

    // Category -> weighted score
    @Builder.Default
    private Map<EventCategory, Integer> categoryAffinity = new EnumMap<>(EventCategory.class);

    // City -> weighted score
    @Builder.Default
    private Map<String, Integer> cityAffinity = new HashMap<>();

    // Tag -> weighted score
    @Builder.Default
    private Map<String, Integer> tagAffinity = new HashMap<>();

    /* =====================
       USER ACTION HISTORY
       ===================== */

    @Builder.Default
    private Set<Long> likedEvents = new HashSet<>();

    @Builder.Default
    private Set<Long> registeredEvents = new HashSet<>();

    // Stored as a plain set of event IDs in MongoDB.
    // The update layer uses $addToSet, which produces an array.
    // A Map<Long, LocalDateTime> is incompatible with that —
    // Spring Data cannot convert an array back into a Map.
    @Builder.Default
    private Set<Long> viewedEvents = new HashSet<>();

    /* =====================
       PRICE PREFERENCE
       ===================== */

    private Double minPreferredPrice;
    private Double maxPreferredPrice;

    /* =====================
       META
       ===================== */

    private LocalDateTime lastActive;

    /* =====================
       HELPER METHODS
       ===================== */

    public void incrementCategory(EventCategory category, int weight) {
        if (category == null) return;
        categoryAffinity.merge(category, weight, Integer::sum);
    }

    public void incrementCity(String city, int weight) {
        if (city == null) return;
        cityAffinity.merge(city, weight, Integer::sum);
    }

    public void incrementTags(List<String> tags, int weight) {
        if (tags == null) return;
        tags.forEach(tag -> tagAffinity.merge(tag, weight, Integer::sum));
    }

    public void addLikedEvent(Long eventId) {
        if (eventId != null) likedEvents.add(eventId);
    }

    public void addRegisteredEvent(Long eventId) {
        if (eventId != null) registeredEvents.add(eventId);
    }

    public void addViewedEvent(Long eventId) {
        if (eventId != null) viewedEvents.add(eventId);
    }

    public boolean hasRegistered(Long eventId) {
        return registeredEvents.contains(eventId);
    }

    public boolean isLiked(Long eventId) {
        return likedEvents.contains(eventId);
    }

    public int getCategoryScore(EventCategory category) {
        return categoryAffinity.getOrDefault(category, 0);
    }

    public int getCityScore(String city) {
        return cityAffinity.getOrDefault(city, 0);
    }

    public int getTagScore(List<String> tags) {
        if (tags == null) return 0;
        return tags.stream()
                .mapToInt(tag -> tagAffinity.getOrDefault(tag, 0))
                .sum();
    }

    public boolean isPriceMatch(Double eventPrice) {
        if (eventPrice == null) return true;
        if (minPreferredPrice == null || maxPreferredPrice == null) return true;
        return eventPrice >= minPreferredPrice && eventPrice <= maxPreferredPrice;
    }
}