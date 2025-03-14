package com.blockchain.EHR.jwt;

import com.blockchain.EHR.services.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserInfoService userInfoService;
    private final ApplicationContext applicationContext;

    @Autowired
    public CustomAuthenticationProvider(UserInfoService userInfoService, ApplicationContext applicationContext) {
        this.userInfoService = userInfoService;
        this.applicationContext = applicationContext;
    }
    
    @Override
public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String username = authentication.getName();
    String password = (String) authentication.getCredentials();

    if (password == null) {
        throw new BadCredentialsException("Password cannot be null");
    }

    String mspId = ((CustomUsernamePasswordAuthenticationToken) authentication).getMspId();

    // Retrieve PasswordEncoder lazily
    PasswordEncoder passwordEncoder = applicationContext.getBean(PasswordEncoder.class);

    // Delegate to UserDetailsService
    UserDetails userDetails = userInfoService.loadUserByUsernameAndMsp(username, mspId);

    if (userDetails != null && passwordEncoder.matches(password, userDetails.getPassword())) {
        return new CustomUsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities(), mspId);
    } else {
        throw new BadCredentialsException("Invalid username or password");
    }
}

    @Override
    public boolean supports(Class<?> authentication) {
        return CustomUsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}