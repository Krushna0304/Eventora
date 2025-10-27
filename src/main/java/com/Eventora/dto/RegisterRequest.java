package com.Eventora.dto;

public record RegisterRequest(
        String displayName,
        String email,
        String password
) {
}
