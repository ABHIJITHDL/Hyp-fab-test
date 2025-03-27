package com.blockchain.EHR.services;

import com.blockchain.EHR.jwt.CustomUserDetails;
import com.blockchain.EHR.model.UserEntity;
import com.blockchain.EHR.repository.UserRepository;
import org.apache.xmlbeans.impl.xb.xsdschema.Attribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserInfoService implements UserDetailsService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserInfoService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserEntity> userEntity = userRepository.findByUsername(username);

        return userEntity.map(CustomUserDetails::new).orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public UserDetails loadUserByUsernameAndMsp(String username,String mspId) throws UsernameNotFoundException {
        Optional<UserEntity> userEntity = userRepository.findByUsernameAndMspId(username,mspId);

        return userEntity.map(CustomUserDetails::new).orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public boolean userExists(String username, String mspId){
        Optional<UserEntity> userEntity = userRepository.findByUsernameAndMspId(username,mspId);
        return userEntity.isPresent();
    }

    public boolean addUser(UserEntity user){
        Optional<UserEntity> userExists = userRepository.findByUsernameAndMspId(user.getUsername(), user.getMspId());
        if(userExists.isPresent()){
            throw new RuntimeException("Username already taken");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return true;
    }
}
