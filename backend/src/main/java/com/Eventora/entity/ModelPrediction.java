package com.Eventora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_predictions", indexes = {
        @Index(name = "idx_prediction_event", columnList = "event_id"),
        @Index(name = "idx_prediction_model", columnList = "model_name")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;

    private String modelName; // e.g., event_success_v1
    private String modelVersion;

    private LocalDateTime predictedAt;

    private Integer predictedLabel; // 0/1

    @Column(columnDefinition = "double precision")
    private Double predictedProbability; // probability of success, 0-1

    // store relevant features used at prediction time (JSON string)
    @Column(columnDefinition = "TEXT")
    private String featuresJson;

    // optional: suggested action (reduce price / promote)
    @Column(length = 100)
    private String suggestedAction;

    @Column(precision = 12, scale = 2)
    private BigDecimal expectedRevenue;

    private LocalDateTime validUntil; // when this prediction becomes stale
}
