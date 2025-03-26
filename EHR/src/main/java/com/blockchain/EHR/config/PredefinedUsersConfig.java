package com.blockchain.EHR.config;

import com.blockchain.EHR.model.UserEntity;
import com.blockchain.EHR.services.UserInfoService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.PostConstruct;

@Configuration
public class PredefinedUsersConfig {
    private final UserInfoService userInfoService;

    public PredefinedUsersConfig(@Lazy UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @PostConstruct
    public void insertPredefinedUsers() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // Use a separate instance
        UserEntity user1 = UserEntity.builder()
                .username("admin")
                .password("adminpw") // Encode password here
                .mspId("Org1MSP")
                .role("ADMIN")
                .build();

        UserEntity user2 = UserEntity.builder()
                .username("admin")
                .password("adminpw") // Encode password here
                .mspId("Org2MSP")
                .role("ADMIN")
                .build();

        if (!userInfoService.userExists("admin", "Org1MSP")) {
            userInfoService.addUser(user1);
            userInfoService.addUser(user2);
        }

        System.out.println("Predefined users inserted.");
    }
}