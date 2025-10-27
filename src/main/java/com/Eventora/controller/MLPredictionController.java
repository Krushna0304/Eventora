
package com.Eventora.controller;

import com.Eventora.dto.PredictionResult;
import com.Eventora.entity.Event;
import com.Eventora.repository.EventRepository;
import com.Eventora.service.MLPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ml")
public class MLPredictionController {

    private static final Logger log = LoggerFactory.getLogger(MLPredictionController.class);
    private final MLPredictionService mlPredictionService;
    private final EventRepository eventRepository;

    public MLPredictionController(MLPredictionService mlPredictionService,EventRepository eventRepository)
    {
        this.mlPredictionService = mlPredictionService;
        this.eventRepository = eventRepository;
    }
    /**
     * Get success prediction for a specific event
     *
     * Example: GET /api/ml/predict/event/123
     */
    @GetMapping("/predict/event/{eventId}")
    public ResponseEntity<PredictionResult> predictEvent(@PathVariable Long eventId) {
        log.info("Prediction requested for event ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        PredictionResult result = mlPredictionService.predictEventSuccess(event);

        return ResponseEntity.ok(result);
    }

    /**
     * Get batch predictions for multiple events
     *
     * Example: POST /api/ml/predict/batch
     * Body: {"eventIds": [1, 2, 3, 4, 5]}
     */
    @PostMapping("/predict/batch")
    public ResponseEntity<List<PredictionResult>> predictBatch(
            @RequestBody Map<String, List<Long>> request) {

        List<Long> eventIds = request.get("eventIds");
        log.info("Batch prediction requested for {} events", eventIds.size());

        List<Event> events = eventRepository.findAllById(eventIds);
        List<PredictionResult> results = mlPredictionService.predictEventsBatch(events);

        return ResponseEntity.ok(results);
    }

    /**
     * Get latest prediction for an event
     *
     * Example: GET /api/ml/prediction/latest/123
     */
    @GetMapping("/prediction/latest/{eventId}")
    public ResponseEntity<PredictionResult> getLatestPrediction(@PathVariable Long eventId) {
        return mlPredictionService.getLatestPrediction(eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all predictions for an event
     *
     * Example: GET /api/ml/prediction/history/123
     */
    @GetMapping("/prediction/history/{eventId}")
    public ResponseEntity<Map<String, Object>> getPredictionHistory(@PathVariable Long eventId) {
        Map<String, Object> history = mlPredictionService.getPredictionHistory(eventId);
        return ResponseEntity.ok(history);
    }

    /**
     * Check ML service health
     *
     * Example: GET /api/ml/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> health = mlPredictionService.checkMLServiceHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * Get ML service statistics
     *
     * Example: GET /api/ml/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = mlPredictionService.getMLServiceStats();
        return ResponseEntity.ok(stats);
    }
}
