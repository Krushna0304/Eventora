package com.Eventora.service;

import com.Eventora.config.MLServiceConfig;
import com.Eventora.dto.*;
import com.Eventora.entity.*;
import com.Eventora.entity.enums.EventCategory;
import com.Eventora.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Data
@RequiredArgsConstructor
public class MLPredictionService {

    private final RestTemplate mlRestTemplate;
    private final ObjectMapper objectMapper;
    private final MLServiceConfig mlServiceConfig;
    private final ModelPredictionRepository predictionRepository;
    private final EventAnalyticsRepository analyticsRepository;

    /**
     * Get success prediction for an event
     */
    public PredictionResult predictEventSuccess(Event event) {
        if (!mlServiceConfig.getEnabled()) {
            log.warn("ML service is disabled. Returning default prediction.");
            return getDefaultPrediction(event);
        }

        try {
            // Check for cached valid prediction
            Optional<ModelPrediction> cachedPrediction = getCachedPrediction(event.getId());
            if (cachedPrediction.isPresent()) {
                log.info("Using cached prediction for event {}", event.getId());
                return convertToResult(cachedPrediction.get());
            }

            // Prepare features
            MLPredictionRequest request = extractFeatures(event);

            // Call ML service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<MLPredictionRequest> httpRequest = new HttpEntity<>(request, headers);

            String url = mlServiceConfig.getUrl() + "/predict";
            log.info("Calling ML service at: {}", url);

            ResponseEntity<MLPredictionResponse> response = mlRestTemplate.postForEntity(
                    url,
                    httpRequest,
                    MLPredictionResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                MLPredictionResponse mlResponse = response.getBody();

                // Save prediction to database
                ModelPrediction prediction = savePrediction(event, mlResponse, request);

                // Save analytics snapshot
                saveAnalyticsSnapshot(event, mlResponse);

                return PredictionResult.builder()
                        .success(true)
                        .eventId(event.getId())
                        .probability(mlResponse.getProbability())
                        .label(mlResponse.getLabel())
                        .confidence(mlResponse.getConfidence())
                        .expectedAttendance(mlResponse.getExpectedAttendance())
                        .expectedRevenue(mlResponse.getExpectedRevenue())
                        .recommendations(mlResponse.getRecommendations())
                        .modelName(mlResponse.getModelName())
                        .modelVersion(mlResponse.getModelVersion())
                        .predictedAt(mlResponse.getPredictedAt())
                        .predictionId(prediction.getId())
                        .build();
            } else {
                log.error("ML service returned non-OK status: {}", response.getStatusCode());
                return getDefaultPrediction(event);
            }

        } catch (RestClientException e) {
            log.error("Error calling ML service: {}", e.getMessage());
            return getDefaultPrediction(event);
        } catch (Exception e) {
            log.error("Unexpected error in prediction: {}", e.getMessage(), e);
            return getDefaultPrediction(event);
        }
    }

    /**
     * Batch prediction for multiple events
     */
    public List<PredictionResult> predictEventsBatch(List<Event> events) {
        if (!mlServiceConfig.getEnabled()) {
            return events.stream()
                    .map(this::getDefaultPrediction)
                    .collect(Collectors.toList());
        }

        try {
            BatchPredictionRequest batchRequest = BatchPredictionRequest.builder()
                    .events(events.stream()
                            .map(this::extractFeatures)
                            .collect(Collectors.toList()))
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<BatchPredictionRequest> httpRequest = new HttpEntity<>(batchRequest, headers);

            String url = mlServiceConfig.getUrl() + "/predict/batch";

            ResponseEntity<BatchPredictionResponse> response = mlRestTemplate.postForEntity(
                    url,
                    httpRequest,
                    BatchPredictionResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                BatchPredictionResponse batchResponse = response.getBody();

                List<PredictionResult> results = new ArrayList<>();
                for (int i = 0; i < events.size() && i < batchResponse.getPredictions().size(); i++) {
                    Event event = events.get(i);
                    MLPredictionResponse mlResponse = batchResponse.getPredictions().get(i);

                    ModelPrediction prediction = savePrediction(event, mlResponse,
                            batchRequest.getEvents().get(i));

                    results.add(PredictionResult.builder()
                            .success(true)
                            .eventId(event.getId())
                            .probability(mlResponse.getProbability())
                            .label(mlResponse.getLabel())
                            .confidence(mlResponse.getConfidence())
                            .expectedAttendance(mlResponse.getExpectedAttendance())
                            .expectedRevenue(mlResponse.getExpectedRevenue())
                            .recommendations(mlResponse.getRecommendations())
                            .modelName(mlResponse.getModelName())
                            .modelVersion(mlResponse.getModelVersion())
                            .predictedAt(mlResponse.getPredictedAt())
                            .predictionId(prediction.getId())
                            .build());
                }

                return results;
            } else {
                log.error("Batch prediction failed with status: {}", response.getStatusCode());
                return events.stream()
                        .map(this::getDefaultPrediction)
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            log.error("Error in batch prediction: {}", e.getMessage(), e);
            return events.stream()
                    .map(this::getDefaultPrediction)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Get latest prediction for an event
     */
    public Optional<PredictionResult> getLatestPrediction(Long eventId) {
        return predictionRepository.findTopByEventIdOrderByPredictedAtDesc(eventId)
                .map(this::convertToResult);
    }

    /**
     * Get all predictions for an event
     */
    public List<PredictionResult> getAllPredictions(Long eventId) {
        return predictionRepository.findByEventId(eventId)
                .stream()
                .map(this::convertToResult)
                .collect(Collectors.toList());
    }

    /**
     * Get prediction history with analytics
     */
    public Map<String, Object> getPredictionHistory(Long eventId) {
        List<ModelPrediction> predictions = predictionRepository.findByEventId(eventId);
        List<EventAnalytics> analytics = analyticsRepository.findByEventId(eventId);

        Map<String, Object> history = new HashMap<>();
        history.put("predictions", predictions.stream()
                .map(this::convertToResult)
                .collect(Collectors.toList()));
        history.put("analytics", analytics);
        history.put("totalPredictions", predictions.size());

        if (!predictions.isEmpty()) {
            history.put("latestProbability", predictions.get(0).getPredictedProbability());
            history.put("averageProbability", predictions.stream()
                    .mapToDouble(ModelPrediction::getPredictedProbability)
                    .average()
                    .orElse(0.0));
        }

        return history;
    }

    /**
     * Extract features from Event entity
     */
    private MLPredictionRequest extractFeatures(Event event) {
        // Calculate posted days before event
        Integer postedDaysBefore = event.getPostedDaysBeforeEvent();
        if (postedDaysBefore == null && event.getPostedAt() != null) {
            postedDaysBefore = (int) ChronoUnit.DAYS.between(
                    event.getPostedAt(),
                    event.getStartDate()
            );
        }
        if (postedDaysBefore == null) {
            postedDaysBefore = 14; // default
        }

        // Get weekday (0 = Monday, 6 = Sunday)
        int weekday = event.getStartDate().getDayOfWeek().getValue() - 1;

        // Normalize city category
        String cityCategory = event.getCityCategory();
        if (cityCategory == null) {
            cityCategory = normalizeCityCategory(event.getCity());
        }

        return MLPredictionRequest.builder()
                .eventId(event.getId())
                .tagsCount(event.getTagsCount() != null ? event.getTagsCount() :
                        (event.getTags() != null ? event.getTags().size() : 0))
                .postedDaysBeforeEvent(postedDaysBefore)
                .promotionSpend(event.getPromotionSpend() != null ? event.getPromotionSpend() : 0L)
                .maxParticipants(event.getMaxParticipants())
                .ticketPrice(event.getPrice() != null ? event.getPrice().doubleValue() : 0.0)
                .organizerReputation(event.getOrganizerReputation() != null ?
                        event.getOrganizerReputation() : 0.5)
                .avgPastAttendanceRate(event.getAvgPastAttendanceRate() != null ?
                        event.getAvgPastAttendanceRate() : 0.5)
                .ctr(normalizeCtr(event.getCtr() != null ? event.getCtr() : 0.0))
                .socialMentions(event.getSocialMentions() != null ? event.getSocialMentions() : 0)
                .weekday(weekday)
                .category(event.getEventCategory().name())
                .city(cityCategory.toLowerCase())
                .build();
    }

    /**
     * Normalize city to category (small, medium, large)
     */
    private String normalizeCityCategory(String city) {
        // List of major cities in India
        Set<String> largeCities = Set.of(
                "mumbai", "delhi", "bangalore", "hyderabad", "chennai", "kolkata",
                "pune", "ahmedabad", "surat", "jaipur"
        );

        Set<String> mediumCities = Set.of(
                "lucknow", "kanpur", "nagpur", "indore", "thane", "bhopal",
                "visakhapatnam", "pimpri", "patna", "vadodara", "ghaziabad", "ludhiana"
        );

        String cityLower = city.toLowerCase();
        if (largeCities.contains(cityLower)) {
            return "large";
        } else if (mediumCities.contains(cityLower)) {
            return "medium";
        } else {
            return "small";
        }
    }

    /**
     * Normalize CTR (Click-Through Rate) to be between 0 and 1
     * ML service expects CTR as a decimal ratio (0.0 to 1.0)
     */
    private Double normalizeCtr(Double ctr) {
        if (ctr == null) {
            return 0.0;
        }
        // Clamp CTR value to be between 0 and 1
        return Math.min(Math.max(ctr, 0.0), 1.0);
    }

    /**
     * Save prediction to database
     */
    private ModelPrediction savePrediction(Event event, MLPredictionResponse mlResponse,
                                           MLPredictionRequest request) {
        try {
            String featuresJson = objectMapper.writeValueAsString(request);

            // Extract primary recommendation if available
            String suggestedAction = null;
            if (mlResponse.getRecommendations() != null && !mlResponse.getRecommendations().isEmpty()) {
                suggestedAction = mlResponse.getRecommendations().get(0).getCategory();
            }

            ModelPrediction prediction = ModelPrediction.builder()
                    .eventId(event.getId())
                    .modelName(mlResponse.getModelName())
                    .modelVersion(mlResponse.getModelVersion())
                    .predictedAt(LocalDateTime.now())
                    .predictedLabel(mlResponse.getLabel())
                    .predictedProbability(mlResponse.getProbability())
                    .featuresJson(featuresJson)
                    .suggestedAction(suggestedAction)
                    .expectedRevenue(BigDecimal.valueOf(mlResponse.getExpectedRevenue()))
                    .validUntil(LocalDateTime.now().plusHours(mlServiceConfig.getCacheDurationHours()))
                    .build();

            return predictionRepository.save(prediction);
        } catch (JsonProcessingException e) {
            log.error("Error serializing features: {}", e.getMessage());
            throw new RuntimeException("Failed to save prediction", e);
        }
    }

    /**
     * Save analytics snapshot
     */
    private void saveAnalyticsSnapshot(Event event, MLPredictionResponse mlResponse) {
        try {
            EventAnalytics analytics = EventAnalytics.builder()
                    .eventId(event.getId())
                    .snapshotAt(LocalDateTime.now())
                    .tagsCount(event.getTagsCount())
                    .postedDaysBeforeEvent(event.getPostedDaysBeforeEvent())
                    .promotionSpend(event.getPromotionSpend())
                    .maxParticipants(event.getMaxParticipants())
                    .ticketPrice(event.getPrice())
                    .organizerReputation(event.getOrganizerReputation())
                    .avgPastAttendanceRate(event.getAvgPastAttendanceRate())
                    .ctr(event.getCtr())
                    .socialMentions(event.getSocialMentions())
                    .weekday(event.getStartDate().getDayOfWeek().getValue() - 1)
                    .cityCategory(event.getCityCategory())
                    .checkedInCount(event.getCheckedInCount())
                    .revenue(event.getRevenue())
                    .success(mlResponse.getLabel())
                    .build();

            analyticsRepository.save(analytics);
        } catch (Exception e) {
            log.error("Error saving analytics snapshot: {}", e.getMessage());
            // Don't throw - analytics failure shouldn't break prediction
        }
    }

    /**
     * Get cached prediction if valid
     */
    private Optional<ModelPrediction> getCachedPrediction(Long eventId) {
        List<ModelPrediction> validPredictions = predictionRepository
                .findByEventIdAndValidUntilAfter(eventId, LocalDateTime.now());

        return validPredictions.stream()
                .max(Comparator.comparing(ModelPrediction::getPredictedAt));
    }

    /**
     * Convert ModelPrediction entity to PredictionResult DTO
     */
    private PredictionResult convertToResult(ModelPrediction prediction) {
        List<Recommendation> recommendations = new ArrayList<>();

        // Parse recommendations from suggested action if available
        if (prediction.getSuggestedAction() != null) {
            recommendations.add(Recommendation.builder()
                    .priority("MEDIUM")
                    .category(prediction.getSuggestedAction())
                    .message("Check cached prediction details")
                    .build());
        }

        return PredictionResult.builder()
                .success(true)
                .eventId(prediction.getEventId())
                .probability(prediction.getPredictedProbability())
                .label(prediction.getPredictedLabel())
                .confidence(calculateConfidence(prediction.getPredictedProbability()))
                .expectedAttendance(calculateExpectedAttendance(prediction))
                .expectedRevenue(prediction.getExpectedRevenue() != null ?
                        prediction.getExpectedRevenue().doubleValue() : 0.0)
                .recommendations(recommendations)
                .modelName(prediction.getModelName())
                .modelVersion(prediction.getModelVersion())
                .predictedAt(prediction.getPredictedAt().toString())
                .predictionId(prediction.getId())
                .build();
    }

    /**
     * Calculate expected attendance from prediction
     */
    private Integer calculateExpectedAttendance(ModelPrediction prediction) {
        try {
            Map<String, Object> features = objectMapper.readValue(
                    prediction.getFeaturesJson(),
                    Map.class
            );
            Integer maxParticipants = (Integer) features.get("maxParticipants");
            return (int) (prediction.getPredictedProbability() * maxParticipants);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Calculate confidence level
     */
    private String calculateConfidence(Double probability) {
        if (probability >= 0.75 || probability <= 0.25) {
            return "HIGH";
        } else if (probability >= 0.6 || probability <= 0.4) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Get default prediction when ML service is unavailable
     */
    private PredictionResult getDefaultPrediction(Event event) {
        // Simple heuristic-based prediction
        double baseProbability = 0.5;

        if (event.getOrganizerReputation() != null) {
            baseProbability += 0.2 * event.getOrganizerReputation();
        }

        if (event.getPrice() != null && event.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            baseProbability += 0.1; // Free events tend to have higher attendance
        }

        double probability = Math.min(0.95, Math.max(0.05, baseProbability));
        int label = probability >= 0.5 ? 1 : 0;
        int expectedAttendance = (int) (probability * event.getMaxParticipants());
        double expectedRevenue = expectedAttendance *
                (event.getPrice() != null ? event.getPrice().doubleValue() : 0.0);

        List<Recommendation> defaultRecs = Arrays.asList(
                Recommendation.builder()
                        .priority("HIGH")
                        .category("SYSTEM")
                        .message("ML service unavailable. Using heuristic prediction.")
                        .build(),
                Recommendation.builder()
                        .priority("MEDIUM")
                        .category("MARKETING")
                        .message("Increase promotion to improve event visibility.")
                        .build()
        );

        return PredictionResult.builder()
                .success(false)
                .eventId(event.getId())
                .probability(probability)
                .label(label)
                .confidence("LOW")
                .expectedAttendance(expectedAttendance)
                .expectedRevenue(expectedRevenue)
                .recommendations(defaultRecs)
                .modelName("heuristic_fallback")
                .modelVersion("1.0.0")
                .predictedAt(LocalDateTime.now().toString())
                .build();
    }

    /**
     * Check ML service health
     */
    public Map<String, Object> checkMLServiceHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            String url = mlServiceConfig.getUrl() + "/health";
            ResponseEntity<Map> response = mlRestTemplate.getForEntity(url, Map.class);

            health.put("available", response.getStatusCode() == HttpStatus.OK);
            health.put("status", response.getBody());
            health.put("url", mlServiceConfig.getUrl());
        } catch (Exception e) {
            health.put("available", false);
            health.put("error", e.getMessage());
            health.put("url", mlServiceConfig.getUrl());
        }

        return health;
    }

    /**
     * Get ML service statistics
     */
    public Map<String, Object> getMLServiceStats() {
        try {
            String url = mlServiceConfig.getUrl() + "/stats";
            ResponseEntity<Map> response = mlRestTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Error fetching ML service stats: {}", e.getMessage());
        }

        return Collections.emptyMap();
    }
}