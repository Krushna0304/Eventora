package com.Eventora.repository;

import com.Eventora.entity.ModelPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ModelPredictionRepository extends JpaRepository<ModelPrediction, Long> {

    List<ModelPrediction> findByEventId(Long eventId);

    Optional<ModelPrediction> findTopByEventIdOrderByPredictedAtDesc(Long eventId);

    List<ModelPrediction> findByEventIdAndValidUntilAfter(Long eventId, LocalDateTime now);

    List<ModelPrediction> findByModelNameAndModelVersion(String modelName, String modelVersion);

    Long countByPredictedAtAfter(LocalDateTime since);
}