package com.Eventora.Utils;

import org.springframework.stereotype.Component;

@Component
public class ApplicationContextUtils {

    public String getLoggedUserEmail()
    {
//         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//         return authentication.getName();
        return "krushnasalbande@gmail.com";
    }
}
