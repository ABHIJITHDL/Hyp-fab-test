package com.blockchain.EHR.config;

import com.blockchain.EHR.jwt.AuthEntryPointJwt;
import com.blockchain.EHR.jwt.AuthTokenFilter;
import com.blockchain.EHR.jwt.CustomAuthenticationProvider;
import com.blockchain.EHR.jwt.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    @Lazy
    private CustomAuthenticationProvider customAuthenticationProvider;

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring SecurityFilterChain");
        http
                .cors(Customizer.withDefaults())  // Use default CORS configuration
                .csrf(csrf -> csrf.disable())  // Disable CSRF more cleanly
                .exceptionHandling(exceptionHandling -> 
                    exceptionHandling.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/*.html", "/*.css", "/*.js", "/script.js").permitAll()
                        .requestMatchers("/fabric/login").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(customAuthenticationProvider)  // Use the customAuthenticationProvider
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
                
        return http.build();
    }

    @Bean
    @Lazy
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        logger.info("Creating AuthenticationManager");
        return configuration.getAuthenticationManager();
    }
}