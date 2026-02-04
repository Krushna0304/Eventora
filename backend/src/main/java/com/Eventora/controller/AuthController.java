package com.Eventora.controller;

import com.Eventora.dto.LoginRequest;
import com.Eventora.dto.RegisterRequest;
import com.Eventora.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/public/api")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(@RequestBody RegisterRequest registerRequest) {
        try {
            authService.createUser(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "User created successfully"));
        } catch (ResponseStatusException ex) {
            // Catch known errors like 409
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("message", ex.getReason()));
        } catch (Exception ex) {
            // Catch any unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Something went wrong: " + ex.getMessage()));
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        try {
            String token = authService.loginUser(loginRequest);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("token", token));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("message", ex.getReason()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Something went wrong: " + ex.getMessage()));
        }
    }


    //need to be authenticated to access this endpoint
    @GetMapping("/getUserInfo")
    public ResponseEntity<?> getUserInfo() {
        try {
            Map<String, String> userInfo = authService.getCurrentUserInfo();
            return ResponseEntity.ok(userInfo);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("message", ex.getReason()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Something went wrong: " + ex.getMessage()));
        }
    }

}
