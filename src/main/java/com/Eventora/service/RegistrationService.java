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
import org.springframework.transaction.annotation.Transactional;
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

    //Done
    @Transactional
    public void registerUserForEvent(Long eventId) throws Exception,ResponseStatusException {
        if (applicationContextUtils.getLoggedUser() == null)
            throw new RuntimeException("No logged user found");

        AppUser appUser = applicationContextUtils.getLoggedUser();

        if (registrationRepository.checkByIdAndUser(eventId, appUser).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"User registered only once for the event");
        }

        int updated = eventRepository.incrementParticipantCountIfAvailable(eventId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event full or not open for registration");
        }

        registrationRepository.save(
                Registration.builder()
                        .event(Event.builder().id(eventId).build()) // lightweight reference
                        .user(appUser)
                        .status(RegistrationStatus.REGISTERED)
                        .build()
        );
    }

    //Done op
    public List<EventTemplate> getRegisteredEvents() {
        AppUser user = applicationContextUtils.getLoggedUser();
        if (user == null)
            throw new RuntimeException("No logged user found");

        return registrationRepository.findRegisteredEventTemplates(user, RegistrationStatus.REGISTERED);
    }

    //Done
    @Transactional
    public void cancelRegistration(Long eventId) {
        AppUser user = applicationContextUtils.getLoggedUser();
        if (user == null)
            throw new RuntimeException("No logged user found");

        int updatedRows = registrationRepository.updateRegistrationStatus(
                eventId,
                user,
                RegistrationStatus.CANCELLED
        );

        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration not found or already cancelled");
        }

        eventRepository.updateParticipantCount(eventId, -1);
    }

    //Done
    public RegistrationStatus checkIsUserRegisteredForEvent(Long eventId, AppUser user) {
        return registrationRepository.findStatusByEventIdAndUser(eventId, user)
                .orElse(RegistrationStatus.NONE);
    }

    //Done
    @Transactional
    public void checkInToEvent(Long eventId) {
        AppUser user = applicationContextUtils.getLoggedUser();
        if (user == null)
            throw new RuntimeException("No logged user found");

        // Try updating registration status atomically
        int updatedRows = registrationRepository.updateRegistrationStatus(
                eventId,
                user,
                RegistrationStatus.REGISTERED,
                RegistrationStatus.CHECKED_IN
        );

        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User not registered or already checked in");
        }

        // Increase checked-in count
        eventRepository.increaseCheckedInCnt(eventId);
    }



    //index on registrations table for event,user combination for faster lookup - 4 lookup - Done
    //no need to reatrive the user,event forcefully when retriving registration - only getRegisteredEvents required event -Done

    //no need to reatrive the organizer forcefully when retriving event
    //apply pagination on getRegisteredEvents

//    solutions

    //1)Add separate query to retrive the template and separate to retrive full event details

}
