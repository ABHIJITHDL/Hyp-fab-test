package com.blockchain.EHR.jwt;
import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
public class CustomUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private final String mspId;

    public CustomUsernamePasswordAuthenticationToken(Object principal, Object credentials,
                                                     Collection<? extends GrantedAuthority> authorities, String mspId) {
        super(principal, credentials, authorities);
        this.mspId = mspId;
    }

}
