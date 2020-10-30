package com.emanmacario.expensely.filters;

import com.emanmacario.expensely.services.ApplicationUserDetailsService;
import com.emanmacario.expensely.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Examines the incoming request for JWT in the Authorization header,
 * and determines if the JWT is valid. If valid, retrieves the corresponding
 * user's details from the user details service, and saves in the security
 * context
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private ApplicationUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Get the Authorization header from the HTTP request
        final String authorizationHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        // Extract the JWT from the header, then extract the username from the JWT payload
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            username = jwtUtil.extractUsername(jwt);
        }

        // Only continue if the security context is not set with an authenticated user
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Retrieve user details from the service
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Check if the JWT is valid
            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }

        // Continue the filter chain
        chain.doFilter(request, response);
    }
}
