package com.blockchain.EHR.jwt;

import com.blockchain.EHR.services.UserInfoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final UserInfoService userInfoService;
    private final JwtUtils jwtUtils;

    @Autowired
    public AuthTokenFilter(@Lazy UserInfoService userInfoService, JwtUtils jwtUtils) {
        this.userInfoService = userInfoService;
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
                String requestURI = request.getRequestURI();

                // Skip token validation for public endpoints
                if (requestURI.equals("/fabric/login") || requestURI.startsWith("/fabric/login")) {
                    filterChain.doFilter(request, response);
                    return;
                }
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                String mspId = jwtUtils.getMspIdFromJwtToken(jwt);

                UserDetails userDetails = userInfoService.loadUserByUsernameAndMsp(username, mspId);

                CustomUsernamePasswordAuthenticationToken authentication = new CustomUsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities(), mspId);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}