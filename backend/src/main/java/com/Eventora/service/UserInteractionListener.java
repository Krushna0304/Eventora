package com.Eventora.service;

import com.Eventora.entity.Event;
import com.Eventora.repository.EventRepository;
import com.Eventora.entity.UserInteraction;
import com.Eventora.entity.enums.InteractionType;
import com.Eventora.dto.UserInteractionEvent;
import com.Eventora.repository.UserInteractionRepository;
import com.Eventora.service.UserInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserInteractionListener {

    private final UserInteractionRepository interactionRepository;
    private final EventRepository eventRepository;
    private final UserInteractionService interactionService;

    @Async
    @EventListener
    public void handleInteraction(UserInteractionEvent event) {

        Event ev = eventRepository.findById(event.eventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (event.type() == InteractionType.UNREGISTER_EVENT) {
            interactionService.removeInteractionAtomic(event.userId(), ev, event.type());
        } else {
            interactionService.applyInteractionAtomic(event.userId(), ev, event.type());
        }
    }

}

