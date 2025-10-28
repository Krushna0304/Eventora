package com.Eventora.controller;

import com.Eventora.entity.AppUser;
import com.Eventora.repository.AppUserRepository;
import com.Eventora.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@RequestMapping("/auth")
public class OAuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String githubClientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String githubClientSecret;

    @Value("${spring.security.oauth2.client.registration.linkedIn.client-id}")
    private String linkedInClientId;

    @Value("${spring.security.oauth2.client.registration.linkedIn.client-secret}")
    private String linkedInClientSecret;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AppUserRepository appUserRepository;

    @GetMapping("/google/code")
    public ResponseEntity<?> handleGoogleCallback(@RequestParam String code) {
        try {
            System.out.println("Received Google OAuth callback with code: " + code.substring(0, Math.min(10, code.length())) + "...");

            // Exchange code for access token
            String tokenEndpoint = "https://oauth2.googleapis.com/token";
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", googleClientId);
            params.add("client_secret", googleClientSecret);
            params.add("redirect_uri", "http://localhost:5173/auth/google/callback");
            params.add("grant_type", "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            System.out.println("Exchanging code for token...");
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenEndpoint, request, Map.class);

            if (tokenResponse.getStatusCode() != HttpStatus.OK || tokenResponse.getBody() == null) {
                System.err.println("Failed to get token response");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Failed to exchange code for token"));
            }

            String accessToken = (String) tokenResponse.getBody().get("access_token");
            System.out.println("Successfully received access token");

            // Get user info using access token (more reliable than id_token validation)
            String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<?> userRequest = new HttpEntity<>(userHeaders);

            ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                    userInfoUrl, HttpMethod.GET, userRequest, Map.class
            );

            if (userInfoResponse.getStatusCode() == HttpStatus.OK && userInfoResponse.getBody() != null) {
                Map<String, Object> userInfo = userInfoResponse.getBody();
                String email = (String) userInfo.get("email");
                String name = (String) userInfo.get("name");

                System.out.println("Retrieved user info for: " + email);

                if (email == null || email.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Email not provided by Google"));
                }

                // Check if user exists, if not create new user
                Optional<AppUser> existingUser = appUserRepository.findByEmail(email);
                if (!existingUser.isPresent()) {
                    System.out.println("Creating new user: " + email);
                    appUserRepository.save(
                            AppUser.builder()
                                    .email(email)
                                    .displayName(name != null ? name : email.split("@")[0])
                                    .password(UUID.randomUUID().toString()) // Random password for OAuth users
                                    .build()
                    );
                } else {
                    System.out.println("User already exists: " + email);
                }

                // Generate JWT token
                String token = jwtUtils.generateToken(email);
                System.out.println("Generated JWT token successfully");
                return ResponseEntity.ok(Map.of("token", token));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Failed to retrieve user information"));

        } catch (Exception e) {
            System.err.println("Google OAuth error: " + e.getMessage());
            e.printStackTrace();

            // Return more specific error message
            String errorMessage = "OAuth authentication failed";
            if (e.getMessage() != null && e.getMessage().contains("invalid_grant")) {
                errorMessage = "Authorization code expired or already used. Please try again.";
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", errorMessage + ": " + e.getMessage()));
        }
    }

    @GetMapping("/github/code")
    public ResponseEntity<?> handleGithubCallback(@RequestParam String code) {
        try {
            // Exchange code for access token
            String tokenEndpoint = "https://github.com/login/oauth/access_token";
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", githubClientId);
            params.add("client_secret", githubClientSecret);
            params.add("redirect_uri", "http://localhost:5173/auth/github/callback");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenEndpoint, request, Map.class);

            if (tokenResponse.getStatusCode() != HttpStatus.OK || tokenResponse.getBody() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Failed to exchange code for token"));
            }

            String accessToken = (String) tokenResponse.getBody().get("access_token");

            if (accessToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Access token not received from GitHub"));
            }

            // Get user info
            String userInfoUrl = "https://api.github.com/user";
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<?> userRequest = new HttpEntity<>(userHeaders);

            ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                    userInfoUrl, HttpMethod.GET, userRequest, Map.class
            );

            if (userInfoResponse.getStatusCode() == HttpStatus.OK && userInfoResponse.getBody() != null) {
                Map<String, Object> userInfo = userInfoResponse.getBody();

                // Get email from separate endpoint if not in profile
                String email = (String) userInfo.get("email");
                if (email == null || email.isEmpty()) {
                    String emailUrl = "https://api.github.com/user/emails";
                    ResponseEntity<List> emailResponse = restTemplate.exchange(
                            emailUrl, HttpMethod.GET, userRequest, List.class
                    );
                    if (emailResponse.getBody() != null && !emailResponse.getBody().isEmpty()) {
                        // Find primary email or use first one
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> emailList = (List<Map<String, Object>>) emailResponse.getBody();

                        Optional<Map<String, Object>> primaryEmail = emailList.stream()
                                .filter(e -> Boolean.TRUE.equals(e.get("primary")))
                                .findFirst();

                        if (primaryEmail.isPresent()) {
                            email = (String) primaryEmail.get().get("email");
                        } else {
                            email = (String) emailList.get(0).get("email");
                        }
                    }
                }

                if (email == null || email.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Email not provided by GitHub. Please make your email public."));
                }

                String name = (String) userInfo.get("name");
                if (name == null || name.isEmpty()) {
                    name = (String) userInfo.get("login");
                }

                // Check if user exists
                Optional<AppUser> existingUser = appUserRepository.findByEmail(email);
                if (!existingUser.isPresent()) {
                    appUserRepository.save(
                            AppUser.builder()
                                    .email(email)
                                    .displayName(name != null ? name : email.split("@")[0])
                                    .password(UUID.randomUUID().toString())
                                    .build()
                    );
                }

                String token = jwtUtils.generateToken(email);
                return ResponseEntity.ok(Map.of("token", token));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Failed to retrieve user information"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "GitHub OAuth failed: " + e.getMessage()));
        }
    }

    @GetMapping("/linkedin/code")
    public ResponseEntity<?> handleLinkedInCallback(@RequestParam String code) {
        try {
            // Exchange code for access token
            String tokenEndpoint = "https://www.linkedin.com/oauth/v2/accessToken";
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", linkedInClientId);
            params.add("client_secret", linkedInClientSecret);
            params.add("redirect_uri", "http://localhost:5173/auth/linkedin/callback");
            params.add("grant_type", "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenEndpoint, request, Map.class);

            if (tokenResponse.getStatusCode() != HttpStatus.OK || tokenResponse.getBody() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Failed to exchange code for token"));
            }

            String accessToken = (String) tokenResponse.getBody().get("access_token");

            if (accessToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Access token not received from LinkedIn"));
            }

            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<?> userRequest = new HttpEntity<>(userHeaders);

            // Get user email using correct API v2 endpoint
            String emailUrl = "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))";
            ResponseEntity<Map> emailResponse = restTemplate.exchange(
                    emailUrl, HttpMethod.GET, userRequest, Map.class
            );

            // Get user profile using correct API v2 endpoint
            String profileUrl = "https://api.linkedin.com/v2/me";
            ResponseEntity<Map> profileResponse = restTemplate.exchange(
                    profileUrl, HttpMethod.GET, userRequest, Map.class
            );

            if (emailResponse.getStatusCode() == HttpStatus.OK &&
                    profileResponse.getStatusCode() == HttpStatus.OK &&
                    emailResponse.getBody() != null &&
                    profileResponse.getBody() != null) {

                Map<String, Object> emailData = emailResponse.getBody();
                Map<String, Object> profileData = profileResponse.getBody();

                // Extract email from response
                String email = null;
                List<Map<String, Object>> elements = (List<Map<String, Object>>) emailData.get("elements");
                if (elements != null && !elements.isEmpty()) {
                    Map<String, Object> handleObj = (Map<String, Object>) elements.get(0).get("handle~");
                    if (handleObj != null) {
                        email = (String) handleObj.get("emailAddress");
                    }
                }

                if (email == null || email.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Email not provided by LinkedIn"));
                }

                // Extract name from profile
                String firstName = "";
                String lastName = "";

                Map<String, Object> localizedFirstName = (Map<String, Object>) profileData.get("localizedFirstName");
                Map<String, Object> localizedLastName = (Map<String, Object>) profileData.get("localizedLastName");

                if (localizedFirstName != null) {
                    firstName = (String) localizedFirstName.getOrDefault("localized", "");
                    // If localized is a map with language key
                    if (firstName.isEmpty() && localizedFirstName.get("en_US") != null) {
                        firstName = (String) localizedFirstName.get("en_US");
                    }
                }

                if (localizedLastName != null) {
                    lastName = (String) localizedLastName.getOrDefault("localized", "");
                    // If localized is a map with language key
                    if (lastName.isEmpty() && localizedLastName.get("en_US") != null) {
                        lastName = (String) localizedLastName.get("en_US");
                    }
                }

                String name = (firstName + " " + lastName).trim();
                if (name.isEmpty()) {
                    name = email.split("@")[0];
                }

                // Check if user exists
                Optional<AppUser> existingUser = appUserRepository.findByEmail(email);
                if (!existingUser.isPresent()) {
                    appUserRepository.save(
                            AppUser.builder()
                                    .email(email)
                                    .displayName(name)
                                    .password(UUID.randomUUID().toString())
                                    .build()
                    );
                }

                String token = jwtUtils.generateToken(email);
                return ResponseEntity.ok(Map.of("token", token));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Failed to retrieve user information"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "LinkedIn OAuth failed: " + e.getMessage()));
        }
    }
}