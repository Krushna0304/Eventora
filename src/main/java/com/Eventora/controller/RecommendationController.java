package com.Eventora.controller;

import com.Eventora.dto.EventTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @GetMapping("/")
    public ResponseEntity<?> getRecommendations()
    {
        try{
            return new ResponseEntity<>(HttpStatus.CONTINUE);
        }catch(Exception ex)
        {
            return new ResponseEntity<>(HttpStatus.CONTINUE);
        }
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<?> getSimilarRecommendations(@PathVariable Long eventId)
    {
        try{
            return new ResponseEntity<>(HttpStatus.CONTINUE);
        }catch(Exception ex)
        {
            return new ResponseEntity<>(HttpStatus.CONTINUE);
        }
    }
}
