package com.Eventora.controller;
import com.Eventora.dto.CreateEventDto;
import com.Eventora.dto.EventDetailDto;
import com.Eventora.dto.EventFilterRequest;
import com.Eventora.dto.EventTemplate;
import com.Eventora.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
@RestController
///api/v1/organizer/events


@RequestMapping("/public/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/getByNameAndOrganizer")
    public ResponseEntity<?> getByNameAndOrganizer(
            @RequestParam(name = "eventName", required = false) String eventName,
            @RequestParam(name = "organizerName", required = false) String organizerName,
            @RequestParam(name = "isMyEventList", required = false, defaultValue = "false") Boolean isMyEventList,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size)
    {
        try {
            Page<EventTemplate> events = eventService.findEventByNameAndOrganizer(
                    eventName,organizerName,isMyEventList,page,size
            );
            return ResponseEntity.ok(events);
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(exception.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while searching events");
        }
    }


    @PostMapping("/getByFilter")
    public ResponseEntity<?> filterEvents(@RequestBody EventFilterRequest filterRequest) {
       try{
        return new ResponseEntity<>(eventService.getFilteredEvents(filterRequest), HttpStatus.FOUND) ;
       } catch (RuntimeException e) {
           e.printStackTrace();
           return new ResponseEntity<>(HttpStatus.NOT_FOUND);
       }
    }

    @GetMapping("/getById")
    public ResponseEntity<?> getEventById(@RequestParam("eventId") Long eventId)
    {
        try{
            return new ResponseEntity<>(eventService.getEventById(eventId), HttpStatus.FOUND) ;
           } catch (Exception e) {
               return new ResponseEntity<>(HttpStatus.NOT_FOUND);
           }
    }

    @PostMapping("/create")

    public ResponseEntity<EventDetailDto> createEvent(@RequestBody CreateEventDto createEventDto) {
        //, @RequestParam("poster")MultipartFile file;
        MultipartFile file = null;
        try {
            EventDetailDto createdEvent = eventService.createEvent(createEventDto,file);
            return ResponseEntity.ok(createdEvent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping ("/cancel/{eventId}")
    public ResponseEntity<?> cancelEvent(@PathVariable Long eventId) {
        try {
            eventService.cancelEvent(eventId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/schedule/{eventId}")
    public ResponseEntity<?> scheduleEvent(@PathVariable Long eventId) {
        try {
            eventService.scheduleEvent(eventId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/update/{eventId}")
    public ResponseEntity<EventDetailDto> modifyEvent(@PathVariable  Long eventId,@RequestBody CreateEventDto updatedEventDto) {
        try {
            MultipartFile file = null;
            EventDetailDto modifyEvent = eventService.modifyEvent(eventId,updatedEventDto,file);
            return ResponseEntity.ok(modifyEvent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/getByNameOrganiserByMe")
    public ResponseEntity<?> getEventByNameOrganiserByMe(
            @RequestParam(name = "eventName", required = false) String eventName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<EventTemplate> events = eventService.getEventByNameOrganiserByMe(
                    eventName,page,size
            ) ;
            return ResponseEntity.ok(events);
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getReason());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error fetching events");
        }
    }

}
