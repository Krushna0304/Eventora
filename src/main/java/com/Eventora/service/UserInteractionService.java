package com.Eventora.service;

import com.Eventora.Utils.ApplicationContextUtils;
import com.Eventora.Utils.EventUtils;
import com.Eventora.dto.EventTemplate;
import com.Eventora.dto.UserInteractionEvent;
import com.Eventora.entity.AppUser;
import com.Eventora.entity.Event;
import com.Eventora.entity.EventWatchlist;
import com.Eventora.entity.UserInteraction;
import com.Eventora.entity.enums.InteractionType;
import com.Eventora.repository.EventRepository;
import com.Eventora.repository.EventWatchlistRepository;
import com.Eventora.repository.UserInteractionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserInteractionService {

    private final UserInteractionRepository userInteractionRepository;
    private final EventWatchlistRepository eventWatchlistRepository;
    private final EventRepository eventRepository;
    private final EventUtils eventUtils;
    private final ApplicationContextUtils applicationContextUtils;
    private final ApplicationEventPublisher publisher;
    private final MongoTemplate mongoTemplate;

    // Per-userId lock objects.
    // Ensures that concurrent @Async events for the SAME user are serialised
    // before they hit MongoDB, preventing the upsert race that creates duplicates.
    // Different users never contend with each other.
    private final ConcurrentHashMap<Long, Object> userLocks = new ConcurrentHashMap<>();

    public UserInteractionService(
            MongoTemplate mongoTemplate,
            ApplicationEventPublisher publisher,
            UserInteractionRepository userInteractionRepository,
            EventWatchlistRepository eventWatchlistRepository,
            EventRepository eventRepository,
            EventUtils eventUtils,
            ApplicationContextUtils applicationContextUtils
    ) {
        this.userInteractionRepository = userInteractionRepository;
        this.eventWatchlistRepository = eventWatchlistRepository;
        this.applicationContextUtils = applicationContextUtils;
        this.eventRepository = eventRepository;
        this.eventUtils = eventUtils;
        this.publisher = publisher;
        this.mongoTemplate = mongoTemplate;
    }

    public List<EventTemplate> getLikedEvents() {
        List<Event> events = eventRepository.findAllByIdIn(getLikedEventIds());
        return events.stream().map(event -> eventUtils.mapToEventTemplate(event, true)).toList();
    }

    public void createLikedEvent(Long eventId) {
        AppUser appUser = applicationContextUtils.getLoggedUser();

        EventWatchlist eventWatchlist = EventWatchlist.builder()
                .event(Event.builder().id(eventId).build())
                .user(appUser)
                .build();
        eventWatchlistRepository.save(eventWatchlist);
        publisher.publishEvent(new UserInteractionEvent(
                applicationContextUtils.getLoggedUser().getId(),
                eventId,
                InteractionType.LIKE_EVENT,
                Map.of()
        ));
    }

    public void deleteLikedEvent(Long eventId) {
        AppUser appUser = applicationContextUtils.getLoggedUser();
        eventWatchlistRepository.deleteByUser_IdAndEvent_Id(appUser.getId(), eventId);
    }

    public List<Long> getLikedEventIds() {
        AppUser appUser = applicationContextUtils.getLoggedUser();
        return eventWatchlistRepository.getLikedEventsIdsByUser(appUser.getId());
    }

    public void applyInteractionAtomic(
            Long userId,
            Event event,
            InteractionType type
    ) {
        // Acquire a per-user lock so that two @Async threads for the same userId
        // never both see "no document exists" and both attempt an upsert-insert.
        Object lock = userLocks.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {

            Query query = Query.query(Criteria.where("userId").is(userId));

            // Only setOnInsert the scalar userId.
            // $inc on dotted paths (e.g. "categoryAffinity.EDUCATION") auto-creates
            // the parent map and sub-key if missing — no explicit initialisation needed,
            // and adding one would re-introduce the ConflictingUpdateOperators error.
            Update update = new Update()
                    .setOnInsert("userId", userId)
                    .set("lastActive", LocalDateTime.now());

            switch (type) {

                case VIEW_EVENT -> {
                    // viewedEvents is a Set<Long> in the entity, so $addToSet is correct.
                    update.addToSet("viewedEvents", event.getId());
                    update.inc("categoryAffinity." + event.getEventCategory(), 1);
                    update.inc("cityAffinity." + event.getCity(), 1);

                    for (String tag : event.getTags()) {
                        update.inc("tagAffinity." + tag, 1);
                    }
                }

                case LIKE_EVENT -> {
                    update.addToSet("likedEvents", event.getId());
                    update.inc("categoryAffinity." + event.getEventCategory(), 5);
                }

                case REGISTER_EVENT -> {
                    update.addToSet("registeredEvents", event.getId());
                    update.inc("categoryAffinity." + event.getEventCategory(), 8);
                }

                case CHECK_IN -> {
                    update.inc("categoryAffinity." + event.getEventCategory(), 12);
                }
            }

            mongoTemplate.findAndModify(
                    query,
                    update,
                    FindAndModifyOptions.options().upsert(true),
                    UserInteraction.class
            );
        }
    }

    public void removeInteractionAtomic(
            Long userId,
            Event event,
            InteractionType type
    ) {
        Object lock = userLocks.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {

            Query query = Query.query(Criteria.where("userId").is(userId));

            Update update = new Update()
                    .set("lastActive", LocalDateTime.now());

            switch (type) {

                case LIKE_EVENT -> {
                    update.pull("likedEvents", event.getId());
                    update.inc("categoryAffinity." + event.getEventCategory(), -5);
                }

                case UNREGISTER_EVENT -> {
                    update.pull("registeredEvents", event.getId());
                    update.inc("categoryAffinity." + event.getEventCategory(), -8);
                }
            }

            mongoTemplate.findAndModify(
                    query,
                    update,
                    FindAndModifyOptions.options().upsert(true),
                    UserInteraction.class
            );
        }
    }
}