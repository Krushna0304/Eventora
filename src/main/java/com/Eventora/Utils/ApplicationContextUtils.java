package com.Eventora.Utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextUtils {

    public String getLoggedUserEmail()
    {
         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         return authentication.getName();
        //return "krushnasalbande@gmail.com";
    }
}
