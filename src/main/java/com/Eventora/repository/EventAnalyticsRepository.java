package com.Eventora.repository;

import com.Eventora.entity.EventAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventAnalyticsRepository extends JpaRepository<EventAnalytics, Long> {

    List<EventAnalytics> findByEventId(Long eventId);

    List<EventAnalytics> findBySnapshotAtAfter(LocalDateTime since);
}