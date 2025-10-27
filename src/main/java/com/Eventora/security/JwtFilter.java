package com.Eventora.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import com.Eventora.repository.AppUserRepository;
import com.Eventora.security.JwtUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.List;

public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtil;
    private final AppUserRepository userRepository;
    public JwtFilter(JwtUtils jwtUtil, AppUserRepository userRepository) {
        this.jwtUtil = jwtUtil; this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        String authHeader = req.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.getSubjectFromToken(token);
                var userOpt = userRepository.findByEmail(email);

                if (userOpt.isPresent()) {
                    var user = userOpt.get();

                    // Create authentication object (no authorities)
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

                    // ✅ Set the authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        // Continue with the filter chain
        chain.doFilter(req, res);
    }

}

