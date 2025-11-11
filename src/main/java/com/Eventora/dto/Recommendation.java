package com.Eventora.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Recommendation {

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("category")
    private String category;

    @JsonProperty("message")
    private String message;

    @JsonProperty("impact")
    private String impact;
}
