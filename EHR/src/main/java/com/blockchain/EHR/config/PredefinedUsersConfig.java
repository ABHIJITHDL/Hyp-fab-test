package com.blockchain.EHR.config;


import com.blockchain.EHR.model.UserEntity;
import com.blockchain.EHR.repository.UserRepository;
import com.blockchain.EHR.services.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class PredefinedUsersConfig {

    @Autowired
    private UserInfoService userInfoService;

    @PostConstruct
    public void insertPredefinedUsers() {
        UserEntity user1 = UserEntity.builder()
                .username("admin")
                .password("adminpw")
                .mspId("Org1MSP")
                .build();

        UserEntity user2 = UserEntity.builder()
                .username("admin")
                .password("adminpw")
                .mspId("Org2MSP")
                .build();

        userInfoService.addUser(user1);
        userInfoService.addUser(user2);

        System.out.println("Predefined users inserted.");
    }
}
