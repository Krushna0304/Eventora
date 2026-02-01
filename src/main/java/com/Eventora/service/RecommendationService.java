package com.Eventora.service;

import com.Eventora.Utils.ApplicationContextUtils;
import com.Eventora.Utils.EventUtils;
import com.Eventora.dto.EventTemplate;
import com.Eventora.entity.AppUser;
import com.Eventora.entity.Event;
import com.Eventora.entity.Registration;
import com.Eventora.repository.AppUserRepository;
import com.Eventora.repository.EventRepository;
import com.Eventora.repository.RegistrationRepository;
import org.springframework.stereotype.Service;

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

    //Number of max similar Events to recommend
    private  int maxSize = 8;

    public RecommendationService(UserInteractionService userInteractionService,AppUserRepository appUserRepository,RegistrationRepository registrationRepository ,EventRepository eventRepository,ApplicationContextUtils applicationContextUtils,EventUtils eventUtils)
    {
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
}
