package com.Eventora.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPredictionRequest {
    @JsonProperty("events")
    private List<MLPredictionRequest> events;
}