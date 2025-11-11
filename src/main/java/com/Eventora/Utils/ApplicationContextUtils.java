package com.Eventora.Utils;

import com.Eventora.entity.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextUtils {
    public AppUser getLoggedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof String && principal.equals("anonymousUser")) {
            return null;
            //throw new RuntimeException("Logged-in user not found");
        }

        if (principal instanceof AppUser appUser) {
            return appUser;
        }

        throw new RuntimeException("Principal is not of type AppUser");
    }
}


