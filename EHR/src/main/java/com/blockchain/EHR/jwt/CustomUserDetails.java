package com.blockchain.EHR.jwt;

import com.blockchain.EHR.model.UserEntity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class CustomUserDetails implements UserDetails {

    private final String username;
    private final String password;
    private final String mspId;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(UserEntity user) {
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.mspId = user.getMspId();
        this.authorities = Stream.of(user.getRole() != null ? user.getRole().split(",") : new String[]{"USER"})
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
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