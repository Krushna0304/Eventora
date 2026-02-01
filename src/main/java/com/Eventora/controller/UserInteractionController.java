package com.Eventora.controller;

import com.Eventora.service.UserInteractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/api/interactions")
public class UserInteractionController {

    public final UserInteractionService userInteractionService;
    public UserInteractionController(UserInteractionService userInteractionService){
        this.userInteractionService = userInteractionService;
    }

    @PostMapping("/{eventId}")
    public ResponseEntity<?> createLikeEvent(@PathVariable Long eventId){
        try{
            userInteractionService.createLikedEvent(eventId);
            return ResponseEntity.ok().build();
        }catch(Exception e){
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<?> deleteLikeEvent(@PathVariable Long eventId){

            userInteractionService.deleteLikedEvent(eventId);
            return ResponseEntity.ok().build();

            //return ResponseEntity.badRequest().build();
    }

    @GetMapping
    public ResponseEntity<?> getLikedEvents(){
        try{
            return ResponseEntity.ok(userInteractionService.getLikedEvents());
        }catch(Exception e){
            return ResponseEntity.badRequest().build();
        }
    }
}
