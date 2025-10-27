package com.Eventora.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPredictionResponse {
    @JsonProperty("predictions")
    private List<MLPredictionResponse> predictions;

    @JsonProperty("total_events")
    private Integer totalEvents;

    @JsonProperty("processed_at")
    private String processedAt;
}