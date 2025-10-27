package com.Eventora.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResult {
    private Boolean success;
    private Long eventId;
    private Double probability;
    private Integer label;
    private String confidence; // HIGH, MEDIUM, LOW
    private Integer expectedAttendance;
    private Double expectedRevenue;
    private List<Recommendation> recommendations;
    private String modelName;
    private String modelVersion;
    private String predictedAt;
    private Long predictionId; // ID from ModelPrediction entity
}
