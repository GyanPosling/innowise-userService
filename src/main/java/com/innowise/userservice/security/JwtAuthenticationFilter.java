package com.innowise.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-USER-ID";
    private static final String USER_ROLE_HEADER = "X-USER-ROLE";
    private static final String USER_EMAIL_HEADER = "X-USER-EMAIL";
    private static final String USERNAME_HEADER = "X-USER-NAME";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String role = request.getHeader(USER_ROLE_HEADER);
            String email = request.getHeader(USER_EMAIL_HEADER);
            String username = request.getHeader(USERNAME_HEADER);
            String userId = request.getHeader(USER_ID_HEADER);
            if (role != null && (email != null || username != null)) {
                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority("ROLE_" + role);
                String principal = email != null ? email : username;
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                userId,
                                Collections.singletonList(authority)
                        );
                if (userId != null) {
                    authToken.setDetails(userId);
                }
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
