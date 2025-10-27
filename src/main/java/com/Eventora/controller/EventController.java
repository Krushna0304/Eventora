package com.Eventora.controller;
import com.Eventora.dto.CreateEventDto;
import com.Eventora.dto.EventDetailDto;
import com.Eventora.dto.EventFilterRequest;
import com.Eventora.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/public/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

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

    @DeleteMapping("/delete/{eventId}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long eventId) {
        try {
            eventService.deleteEvent(eventId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/update/{eventId}")
    public ResponseEntity<EventDetailDto> modifyEvent(@PathVariable  Long eventId,@RequestBody CreateEventDto updatedEventDto, @RequestParam("poster")MultipartFile file) {
        try {
            EventDetailDto modifyEvent = eventService.modifyEvent(eventId,updatedEventDto,file);
            return ResponseEntity.ok(modifyEvent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
