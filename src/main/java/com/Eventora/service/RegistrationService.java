package com.Eventora.service;

import com.Eventora.Utils.ApplicationContextUtils;
import com.Eventora.Utils.EventUtils;
import com.Eventora.dto.EventTemplate;
import com.Eventora.entity.*;
import com.Eventora.entity.enums.EventStatus;
import com.Eventora.entity.enums.RegistrationStatus;
import com.Eventora.repository.EventRepository;
import com.Eventora.repository.AppUserRepository;
import com.Eventora.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final AppUserRepository userRepository;
    private final ApplicationContextUtils applicationContextUtils;
    private final EventUtils eventUtils;

    public void registerUserForEvent(Long eventId) throws Exception,ResponseStatusException {
        String userEmail = applicationContextUtils.getLoggedUserEmail();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new Exception("Event not found"));
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new Exception("User not found"));

        if (registrationRepository.findByEventAndUser(event, user).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"User registered only once for the event");
        }

        if(event.getEventStatus() != EventStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot register for this event");
        }

        if (event.getCurrentParticipants() >= event.getMaxParticipants()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Event capacity full");
        }

        Registration registration = Registration.builder()
                .event(event)
                .user(user)
                .status(RegistrationStatus.REGISTERED)
                .build();

        // Update event participant count
        event.setCurrentParticipants(event.getCurrentParticipants() + 1);
        eventRepository.save(event);

        registrationRepository.save(registration);
    }

    //Discussed with mentor, changed method name to getRegisteredEvents
    public List<EventTemplate> getRegisteredEvents() throws Exception {
     String userEmail = applicationContextUtils.getLoggedUserEmail();
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new Exception("User not found"));
        List<Registration> registrations = registrationRepository.findByUserAndStatus(user,RegistrationStatus.REGISTERED);
        List<Event> registeredEvents = registrations.stream()
                .map(Registration::getEvent)
                .toList();
        return eventUtils.extractEventTemplates(registeredEvents);
    }


    public void cancelRegistration(Long eventId) throws Exception,ResponseStatusException {
        String userEmail = applicationContextUtils.getLoggedUserEmail();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new Exception("Event not found"));
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new Exception("User not found"));

        Registration registration = registrationRepository.findByEventAndUser(event, user)
                .orElseThrow(() -> new Exception("Registration not found"));

        if(registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Registration already cancelled");
        }
        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);

        event.setCurrentParticipants(event.getCurrentParticipants() - 1);
        eventRepository.save(event);
    }

    public RegistrationStatus checkIsUserRegisteredForEvent(Event event, AppUser user) {
        Optional<Registration> registration = registrationRepository.findByEventAndUser(event, user);
        if(registration.isPresent()) {;
            return registration.get().getStatus();
        }
        return RegistrationStatus.NONE;
    }

    public void checkInToEvent(Long eventId) throws Exception,ResponseStatusException {
        String userEmail = applicationContextUtils.getLoggedUserEmail();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new Exception("Event not found"));
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new Exception("User not found"));

        Registration registration = registrationRepository.findByEventAndUser(event, user)
                .orElseThrow(() -> new Exception("Registration not found"));

        if(registration.getStatus() != RegistrationStatus.REGISTERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"User is not registered for the event");
        }

        event.setCheckedInCount(event.getCheckedInCount() + 1);
        registration.setStatus(RegistrationStatus.CHECKED_IN);
        registrationRepository.save(registration);
    }
}
