package com.Eventora.controller;
import com.Eventora.dto.EventTemplate;
import com.Eventora.entity.Event;
import com.Eventora.entity.Registration;
import com.Eventora.service.EventService;
import com.Eventora.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;
    private final EventService eventService;

    @PostMapping("/register-event/{eventId}")
    public ResponseEntity<?> registerForEvent(@PathVariable Long eventId){
        try{
            registrationService.registerUserForEvent(eventId);
            return ResponseEntity.ok("Registered successfully");

        }catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(ex.getReason());
        }catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }

    @GetMapping("/getMyEvents")
    public ResponseEntity<List<EventTemplate>> getRegisteredEvents() throws Exception {
        try
        {
            return ResponseEntity.ok(registrationService.getRegisteredEvents());
        } catch (Exception e) {
            return ResponseEntity.noContent().build();
        }
    }

    @DeleteMapping("/unregister-event/{eventId}")
    public ResponseEntity<String> cancelRegistration(@PathVariable Long eventId)  {
        try{
            registrationService.cancelRegistration(eventId);
            return ResponseEntity.ok("Registration cancelled successfully");
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(ex.getReason());
        }catch (Exception e) {
            String errorMessage = e.getMessage();
            return ResponseEntity.badRequest().body(errorMessage);
        }
    }

    @PutMapping("/check-In/{eventId}")
    public ResponseEntity<String> checkInToEvent(@PathVariable Long eventId) {
        try {
            registrationService.checkInToEvent(eventId);
            return ResponseEntity.ok("Checked in successfully");
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(ex.getReason());
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            return ResponseEntity.badRequest().body(errorMessage);
        }
    }

    @GetMapping("/health-check")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello from RegistrationController");
    }
}
