package com.Eventora.controller;

import com.Eventora.entity.AppUser;
import com.Eventora.repository.AppUserRepository;
import com.Eventora.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenEndpoint, request, Map.class);

            if (tokenResponse.getStatusCode() != HttpStatus.OK || tokenResponse.getBody() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Failed to exchange code for token"));
            }

            String idToken = (String) tokenResponse.getBody().get("id_token");

            // Get user info from ID token
            String userInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            ResponseEntity<Map> userInfoResponse = restTemplate.getForEntity(userInfoUrl, Map.class);

            if (userInfoResponse.getStatusCode() == HttpStatus.OK && userInfoResponse.getBody() != null) {
                Map<String, Object> userInfo = userInfoResponse.getBody();
                String email = (String) userInfo.get("email");
                String name = (String) userInfo.get("name");

                // Check if user exists, if not create new user
                if (!appUserRepository.findByEmail(email).isPresent()) {
                    appUserRepository.save(
                            AppUser.builder()
                                    .email(email)
                                    .displayName(name)
                                    .password(UUID.randomUUID().toString()) // Random password for OAuth users
                                    .build()
                    );
                }

                // Generate JWT token
                String token = jwtUtils.generateToken(email);
                return ResponseEntity.ok(Map.of("token", token));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Failed to retrieve user information"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "OAuth authentication failed: " + e.getMessage()));
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
                        Map<String, Object> primaryEmail = (Map<String, Object>) emailResponse.getBody().stream()
                                .filter(e -> ((Map<String, Object>) e).get("primary").equals(true))
                                .findFirst()
                                .orElse(emailResponse.getBody().get(0));
                        email = (String) primaryEmail.get("email");
                    }
                }

                String name = (String) userInfo.get("name");
                if (name == null || name.isEmpty()) {
                    name = (String) userInfo.get("login");
                }

                // Check if user exists
                if (!appUserRepository.findByEmail(email).isPresent()) {
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
                    .body(Map.of("message", "GitHub OAuth failed: " + e.getMessage()));
        }
    }

    @GetMapping("/linkedIn/code")
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

            // Get user email
            String emailUrl = "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))";
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<?> userRequest = new HttpEntity<>(userHeaders);

            ResponseEntity<Map> emailResponse = restTemplate.exchange(
                    emailUrl, HttpMethod.GET, userRequest, Map.class
            );

            // Get user profile
            String profileUrl = "https://api.linkedin.com/v2/me";
            ResponseEntity<Map> profileResponse = restTemplate.exchange(
                    profileUrl, HttpMethod.GET, userRequest, Map.class
            );

            if (emailResponse.getStatusCode() == HttpStatus.OK &&
                    profileResponse.getStatusCode() == HttpStatus.OK) {

                Map<String, Object> emailData = emailResponse.getBody();
                Map<String, Object> profileData = profileResponse.getBody();

                // Extract email
                List<Map<String, Object>> elements = (List<Map<String, Object>>) emailData.get("elements");
                String email = null;
                if (elements != null && !elements.isEmpty()) {
                    Map<String, Object> handle = (Map<String, Object>) elements.get(0).get("handle~");
                    email = (String) handle.get("emailAddress");
                }

                // Extract name
                String firstName = (String) ((Map<String, Object>) profileData.get("localizedFirstName")).get("localized");
                String lastName = (String) ((Map<String, Object>) profileData.get("localizedLastName")).get("localized");
                String name = firstName + " " + lastName;

                // Check if user exists
                if (!appUserRepository.findByEmail(email).isPresent()) {
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