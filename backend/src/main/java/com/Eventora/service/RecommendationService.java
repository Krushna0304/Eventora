package com.Eventora.service;

import com.Eventora.Utils.ApplicationContextUtils;
import com.Eventora.Utils.EventUtils;
import com.Eventora.dto.EventFilterRequest;
import com.Eventora.dto.EventTemplate;
import com.Eventora.entity.AppUser;
import com.Eventora.entity.Event;
import com.Eventora.entity.Registration;
import com.Eventora.entity.UserInteraction;
import com.Eventora.repository.AppUserRepository;
import com.Eventora.repository.EventRepository;
import com.Eventora.repository.RegistrationRepository;
import com.Eventora.repository.UserInteractionRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
@Service
public class RecommendationService {
    private final ApplicationContextUtils applicationContextUtils;
    private final EventRepository eventRepository;
    private final AppUserRepository appUserRepository;
    private final EventUtils eventUtils;
    private final RegistrationRepository registrationRepository;
    private final UserInteractionService userInteractionService;
    private final UserInteractionRepository interactionRepository;
    private final EventService eventService;
    //Number of max similar Events to recommend
    private  int maxSize = 8;

    public RecommendationService(EventService eventService,UserInteractionService userInteractionService,UserInteractionRepository interactionRepository,AppUserRepository appUserRepository,RegistrationRepository registrationRepository ,EventRepository eventRepository,ApplicationContextUtils applicationContextUtils,EventUtils eventUtils)
    {
        this.eventService = eventService;
        this.interactionRepository = interactionRepository;
        this.userInteractionService = userInteractionService;
        this.appUserRepository = appUserRepository;
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.applicationContextUtils = applicationContextUtils;
        this.eventUtils = eventUtils;
    }
    public List<EventTemplate> getRecommendedEvents()
    {
        if (applicationContextUtils.getLoggedUser() == null)
            throw new RuntimeException("No logged user found");

        AppUser appUser = applicationContextUtils.getLoggedUser();

        List<Registration> registrations = registrationRepository.findByUser(appUser);
//      List<Event> events = re;
//      return eventUtils.extractEventTemplates(events);
        return null;
    }
    public List<EventTemplate> getSimilarEvents(final Long eventId)
    {
        List<Event> events = eventRepository.findAll();
        Event temp = null;
        for(Event e: events)
        {
            if(e.getId().equals(eventId)){
                temp = e;
            }
        }
        final Event currentEvent = temp;
        List<Event> similarEvents = events.stream()
                                    .filter(e -> ! e.getId().equals(eventId))
                .sorted(Comparator.comparingDouble(e-> -similarity(currentEvent,e)))
                                    .limit(maxSize)
                                    .toList();
        List<Long> likedEventIds =  userInteractionService.getLikedEventIds();
        return eventUtils.extractEventTemplates(events, likedEventIds);
    }
    private double similarity(Event e1, Event e2)
    {
        Set<String> tags1 = new HashSet<>(e1.getTags());
        Set<String> tags2 = new HashSet<>(e2.getTags());

        Set<String> intersection = new HashSet<>(tags1);
        intersection.retainAll(tags2);
        return (double) intersection.size() / (Math.sqrt(tags1.size()) * Math.sqrt(tags2.size()));
    }

    /* =====================
       WEIGHTS
       ===================== */
    private static final double SHOWN_PENALTY = 0.30;

    private static final double CATEGORY_W = 0.25;
    private static final double CITY_W = 0.10;
    private static final double TAG_W = 0.20;
    private static final double PRICE_W = 0.10;
    private static final double POPULARITY_W = 0.10;
    private static final double ORGANIZER_W = 0.10;
    private static final double FRESHNESS_W = 0.10;
    private static final double PROMOTION_W = 0.05;

    public List<EventTemplate> recommend( int limit) {



        try{
             AppUser user = applicationContextUtils.getLoggedUser() ;
            if(user == null)
                throw new RuntimeException("No logged user found");

            Long userId = applicationContextUtils.getLoggedUser().getId();

            UserInteraction interaction = interactionRepository
                    .findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User interaction not found"));

        List<Long> likedEventIds = new ArrayList<>(interaction.getLikedEvents());
        List<Event> candidates = eventRepository.findUpcomingEvents();

        List<Event> rankedEvents =  candidates.stream()
                .filter(e -> !interaction.hasRegistered(e.getId()))
                .map(e -> {
                    double score = calculateScore(e, interaction);

                    if (interaction.getShownEvents().contains(e.getId())) {
                        score *= SHOWN_PENALTY; // push down, not remove
                    }

                    return Map.entry(e, score);
                }).sorted(Map.Entry.<Event, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        interaction.addShownEvents(
                rankedEvents.stream().map(Event::getId).toList()
        );

        interactionRepository.save(interaction);

        return eventUtils.extractEventTemplates(rankedEvents, likedEventIds);
        }catch(Exception ex){
            EventFilterRequest eventFilterRequest = new EventFilterRequest();
            return eventService.getFilteredEvents(eventFilterRequest);
        }
    }
    private double calculateScore(Event event, UserInteraction ui) {

        double score = 0.0;

        score += normalize(ui.getCategoryScore(event.getEventCategory()))
                * CATEGORY_W;

        score += normalize(ui.getCityScore(event.getCity()))
                * CITY_W;

        score += normalize(ui.getTagScore(event.getTags()))
                * TAG_W;

        score += priceScore(event, ui) * PRICE_W;

        score += popularityScore(event) * POPULARITY_W;

        score += organizerScore(event) * ORGANIZER_W;

        score += freshnessScore(event) * FRESHNESS_W;

        score += promotionScore(event) * PROMOTION_W;

        return score;
    }
    private double normalize(int rawScore) {
        return Math.min(rawScore / 10.0, 1.0);
    }

    private double priceScore(Event event, UserInteraction ui) {
        if (event.getPrice() == null) return 0.5;

        double price = event.getPrice().doubleValue();
        return ui.isPriceMatch(price) ? 1.0 : 0.0;
    }
    private double popularityScore(Event event) {

        double ctrScore = event.getCtr() != null ? event.getCtr() : 0.0;

        double attendanceRate =
                event.getMaxParticipants() > 0
                        ? (double) event.getCurrentParticipants() / event.getMaxParticipants()
                        : 0.0;

        return clamp((ctrScore + attendanceRate) / 2);
    }

    private double organizerScore(Event event) {

        double reputation = event.getOrganizerReputation() != null
                ? event.getOrganizerReputation()
                : 0.0;

        double pastAttendance = event.getAvgPastAttendanceRate() != null
                ? event.getAvgPastAttendanceRate()
                : 0.0;

        return clamp((reputation + pastAttendance) / 2);
    }
    private double freshnessScore(Event event) {

        long daysUntilEvent =
                ChronoUnit.DAYS.between(LocalDateTime.now(), event.getStartDate());

        if (daysUntilEvent <= 0) return 0.0;
        if (daysUntilEvent <= 3) return 1.0;
        if (daysUntilEvent <= 7) return 0.8;
        if (daysUntilEvent <= 30) return 0.5;

        return 0.2;
    }
    private double promotionScore(Event event) {
        if (event.getPromotionSpend() == null) return 0.0;
        return clamp(event.getPromotionSpend() / 10_000.0);
    }

    private double clamp(double val) {
        return Math.max(0.0, Math.min(1.0, val));
    }

    public List<EventTemplate> findSimilarEvents(
            Long eventId,
            int limit
    ) {
        AppUser user = applicationContextUtils.getLoggedUser();
        if(applicationContextUtils.getLoggedUser() == null)
            throw new RuntimeException("No logged user found");
        Long userId = applicationContextUtils.getLoggedUser().getId();

        Event source = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        UserInteraction interaction = interactionRepository
                .findByUserId(userId)
                .orElse(null);

        List<Long> likedEventIds = interaction != null
                ? new ArrayList<>(interaction.getLikedEvents())
                : List.of();

        List<Event> candidates = eventRepository.findUpcomingEvents();

        List<Event> ranked = candidates.stream()
                .filter(e -> !e.getId().equals(eventId))
                .map(e -> Map.entry(e, similarityScore(source, e)))
                .sorted(Map.Entry.<Event, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        return eventUtils.extractEventTemplates(ranked, likedEventIds);
    }
    private double similarityScore(Event a, Event b) {

        double score = 0.0;

        score += categorySimilarity(a, b) * 0.30;
        score += citySimilarity(a, b) * 0.15;
        score += tagSimilarity(a, b) * 0.25;
        score += priceSimilarity(a, b) * 0.10;
        score += dateSimilarity(a, b) * 0.10;
        score += popularitySimilarity(a, b) * 0.10;

        return score;
    }
    private double categorySimilarity(Event a, Event b) {
        return a.getEventCategory() == b.getEventCategory() ? 1.0 : 0.0;
    }
    private double citySimilarity(Event a, Event b) {
        return Objects.equals(a.getCity(), b.getCity()) ? 1.0 : 0.0;
    }
    private double tagSimilarity(Event a, Event b) {

        if (a.getTags() == null || b.getTags() == null) return 0.0;

        Set<String> setA = new HashSet<>(a.getTags());
        Set<String> setB = new HashSet<>(b.getTags());

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        return union.isEmpty()
                ? 0.0
                : (double) intersection.size() / union.size();
    }
    private double priceSimilarity(Event a, Event b) {

        if (a.getPrice() == null || b.getPrice() == null) return 0.5;

        double diff = Math.abs(a.getPrice().doubleValue() - b.getPrice().doubleValue());
        double max = Math.max(a.getPrice().doubleValue(), b.getPrice().doubleValue());

        return clamp(1 - (diff / max));
    }
    private double dateSimilarity(Event a, Event b) {

        long days = Math.abs(
                ChronoUnit.DAYS.between(a.getStartDate(), b.getStartDate())
        );

        if (days <= 1) return 1.0;
        if (days <= 3) return 0.8;
        if (days <= 7) return 0.6;
        if (days <= 30) return 0.3;

        return 0.1;
    }
    private double popularitySimilarity(Event a, Event b) {

        int diff = Math.abs(
                a.getCurrentParticipants() - b.getCurrentParticipants()
        );

        int max = Math.max(
                a.getCurrentParticipants(), b.getCurrentParticipants()
        );

        if (max == 0) return 0.5;

        return clamp(1 - ((double) diff / max));
    }



}
