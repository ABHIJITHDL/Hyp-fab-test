package com.blockchain.EHR.jwt;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
@Getter
public class CustomUserDetails implements UserDetails {

    private final String username;
    private final String password;
    private final String mspId;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(String username, String password, String mspId, Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.mspId = mspId;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

}