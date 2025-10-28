package com.Eventora.service;

import com.Eventora.Utils.ApplicationContextUtils;
import com.Eventora.dto.LoginRequest;
import com.Eventora.dto.RegisterRequest;
import com.Eventora.entity.AppUser;
import com.Eventora.repository.AppUserRepository;
import com.Eventora.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    private final AppUserRepository appUserRepository;
    private final JwtUtils jwtUtils;
    private final ApplicationContextUtils applicationContextUtils;
    public AuthService(AppUserRepository appUserRepository,JwtUtils jwtUtils,ApplicationContextUtils applicationContextUtils)
    {
        this.appUserRepository = appUserRepository;
        this.jwtUtils = jwtUtils;
        this.applicationContextUtils = applicationContextUtils;
    }

    public void createUser(RegisterRequest registerRequest) {
        Optional<AppUser> existingUser = appUserRepository.findByEmail(registerRequest.email());

        if (existingUser.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        AppUser newUser = AppUser.builder()
                .displayName(registerRequest.displayName())
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .build();

        appUserRepository.save(newUser);
    }


    public String loginUser(LoginRequest loginRequest) {
        AppUser appUser = appUserRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "User does not exist"));

        // Compare encoded password correctly
        if (!passwordEncoder.matches(loginRequest.password(), appUser.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Credentials");
        }

        return jwtUtils.generateToken(loginRequest.email());
    }

    public Map<String, String> getCurrentUserInfo() {
        String email = applicationContextUtils.getLoggedUserEmail();
        AppUser appUser = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Map <String, String> userInfo = Map.of(
                "displayName", appUser.getDisplayName(),
                "email", appUser.getEmail()
        );
        return userInfo;
    }

}
