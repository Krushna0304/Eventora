package com.Eventora.dto;

public record LoginRequest(
        String email,
        String password
) {
}
