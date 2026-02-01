package com.Eventora.dto;


import com.Eventora.entity.enums.InteractionType;

import java.util.Map;

public record UserInteractionEvent(
        Long userId,
        Long eventId,
        InteractionType type,
        Map<String, Object> metadata
) {}

