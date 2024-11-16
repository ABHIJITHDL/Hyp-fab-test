package com.blockchain.EHR.jwt;

import com.blockchain.EHR.services.FabricUserRegistration;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.blockchain.EHR.services.FabricUserRegistration.getCAConfig;
import static org.bouncycastle.asn1.x509.X509ObjectIdentifiers.organization;

@Component
public class CAAuthenticationProvider implements AuthenticationProvider {

    FabricUserRegistration fabricUserRegistration;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        String mspId = ((CustomUsernamePasswordAuthenticationToken) authentication).getMspId();
        try {
            Map<String,String> caConfig=getCAConfig(mspId.toLowerCase().replace("MSP",""));
            String CA_URL=caConfig.get("CA_URL");
            String CA_CERT_PATH = caConfig.get("CA_CERT_PATH");
            // Step 1: Create the CA client
            Properties props = new Properties();
            props.put("pemFile", CA_CERT_PATH);
            props.put("allowAllHostNames", "true");
            HFCAClient caClient = HFCAClient.createNewInstance(CA_URL, props);
            caClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

            // Step 2: Authenticate the user
            boolean isAuthenticated = fabricUserRegistration.authenticateUser(username, password, mspId.toLowerCase().replace("MSP",""));
            if (!isAuthenticated) {
                throw new BadCredentialsException("Invalid username or password");
            }

            // Step 3: Get user's role and MSP ID
            String role = FabricUserRegistration.getRole(caClient, username);

            // Step 4: Map role to GrantedAuthority
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(role));

            // Return the authenticated token with roles and MSP ID
            return new CustomUsernamePasswordAuthenticationToken(username, password, authorities, mspId);
        } catch (Exception e) {
            throw new BadCredentialsException("Authentication failed", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
