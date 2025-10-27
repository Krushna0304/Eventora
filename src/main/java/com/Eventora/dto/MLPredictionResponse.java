package com.Eventora.dto;

import lombok.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLPredictionResponse {

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("event_id")
    private Long eventId;

    @JsonProperty("probability")
    private Double probability;

    @JsonProperty("label")
    private Integer label;

    @JsonProperty("confidence")
    private String confidence;

    @JsonProperty("expected_attendance")
    private Integer expectedAttendance;

    @JsonProperty("expected_revenue")
    private Double expectedRevenue;

    @JsonProperty("recommendations")
    private List<Recommendation> recommendations;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("predicted_at")
    private String predictedAt;
}
