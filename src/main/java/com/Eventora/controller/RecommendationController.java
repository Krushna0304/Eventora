package com.Eventora.controller;

import com.Eventora.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    public final RecommendationService recommendationService;

    @GetMapping()
    public ResponseEntity<?> getRecommendations(@RequestParam int limit)
    {
        try{
            return new ResponseEntity<>(recommendationService.recommend(limit),HttpStatus.OK);
        }catch(Exception ex)
        {
            return new ResponseEntity<>(HttpStatus.CONTINUE);
        }
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<?> getSimilarRecommendations(@PathVariable Long eventId)
    {
        try{
            return new ResponseEntity<>(recommendationService.findSimilarEvents(eventId,10),HttpStatus.OK);
        }catch(Exception ex)
        {
            return new ResponseEntity<>(HttpStatus.CONTINUE);
        }
    }
}
