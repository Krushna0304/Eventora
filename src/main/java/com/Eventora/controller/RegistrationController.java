package com.Eventora.controller;
import com.Eventora.dto.EventTemplate;
import com.Eventora.entity.Registration;
import com.Eventora.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
    @RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/register-event/{eventId}")
    public ResponseEntity<?> registerForEvent(@PathVariable Long eventId){
        try{
            registrationService.registerUserForEvent(eventId);
            return ResponseEntity.ok("Registered successfully");
        } catch (Exception e) {
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
    public ResponseEntity<String> cancelRegistration(@PathVariable Long eventId) throws Exception {
        registrationService.cancelRegistration(eventId);
        return ResponseEntity.ok("Registration cancelled successfully");
    }
}
