package com.Eventora.dto;

import com.Eventora.entity.enums.EventCategory;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EventFilterRequest {
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String city;
    private String state;
    private String country;
    private EventCategory eventCategory;
    private Double latitude;
    private Double longitude;
    private Double radiusInKm; // optional if you want nearby events
}
