package com.blockchain.EHR.controller;

import com.blockchain.EHR.jwt.CustomUsernamePasswordAuthenticationToken;
import com.blockchain.EHR.jwt.JwtUtils;
import com.blockchain.EHR.model.EhrDocument;
import com.blockchain.EHR.model.LoginRequest;
import com.blockchain.EHR.model.UserEntity;
import com.blockchain.EHR.services.EhrService;
import com.blockchain.EHR.services.FabricService;
import com.blockchain.EHR.services.FabricUserRegistration;
import com.blockchain.EHR.services.UserInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/fabric")
@AllArgsConstructor
public class FabricController {
    private final EhrService ehrService;
    private final FabricService fabricService;
    private final UserInfoService userInfoService;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final FabricUserRegistration fabricUserRegistration;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public String login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new CustomUsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword(),
                        null,
                        loginRequest.getMspId()
                )
        );

        if (authentication.isAuthenticated()) {
            System.out.println("User " + loginRequest.getUsername() + " is authenticated");
            return jwtUtils.generateTokenFromUserDetails(loginRequest.getUsername(), loginRequest.getMspId());
        } else {
            throw new UsernameNotFoundException("Invalid user request");
        }
    }

    @PostMapping("/submit")
    public String submitTransaction(HttpServletRequest request,
                                    @RequestParam String channelName,
                                    @RequestParam String chaincodeName,
                                    @RequestParam String functionName,
                                    @RequestParam String... args){
        String jwt = jwtUtils.getJwtFromHeader(request);
        System.out.println("jwt received");
        String username = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);
        return fabricService.submitTransaction(channelName, chaincodeName, functionName, args,username,mspId);
    }

    @GetMapping("/query")
    public String queryTransaction(HttpServletRequest request,
                                   @RequestParam String channelName,
                                    @RequestParam String chaincodeName,
                                    @RequestParam String functionName,
                                    @RequestParam String... args){
        String jwt = jwtUtils.getJwtFromHeader(request);
        String username = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);
        return fabricService.evaluateTransaction(channelName, chaincodeName, functionName, args,username,mspId);
    }


    @PostMapping("/enrollAdmin")
    public String enrollAdmin() {
        return "Admin enrolled successfully";
    }


    @PostMapping("/register")
    public String enrollUser(HttpServletRequest request,
                             @RequestParam String username,
                             @RequestParam String password,
                             @RequestParam(value = "file",required = false ) MultipartFile file) {

        String jwt = jwtUtils.getJwtFromHeader(request);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);
        String id = jwtUtils.getUserNameFromJwtToken(jwt);
        System.out.println("Register controller");
        if(file!=null){
            if(!"Org2MSP".equals(mspId) )
                return "Only Patient Admin can Upload pdf";
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                EhrDocument ehrDocument = objectMapper.readValue(file.getInputStream(),EhrDocument.class);
                ehrService.addEhrDocument(username, ehrDocument);
            } catch (Exception e) {
                System.err.println("Error during PDF upload: " + e.getMessage());
                return "Error During uploading pdf";
            }
        }

        System.out.println("Received");
        if(userInfoService.addUser(UserEntity.builder()
                .username(username)
                .password(password)
                .mspId(mspId)
                .build()) && fabricUserRegistration.addUser(username,password,mspId))
            return "User registered successfully";
        else
            return  "User registration failed";
    }
}
