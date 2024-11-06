package com.blockchain.EHR.controller;

import com.blockchain.EHR.jwt.CustomUserDetails;
import com.blockchain.EHR.jwt.JwtUtils;
import com.blockchain.EHR.services.FabricService;
import com.blockchain.EHR.services.FabricUserRegistration;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;

@RestController
@RequestMapping("/fabric")
public class FabricController {

    private final FabricService fabricService;
    private final FabricUserRegistration fabricUserRegistration;
    private final JwtUtils jwtUtils;

    public FabricController(FabricService fabricService, FabricUserRegistration fabricUserRegistration, JwtUtils jwtUtils) {
        this.fabricService = fabricService;
        this.fabricUserRegistration = fabricUserRegistration;
        this.jwtUtils = jwtUtils;
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password, @RequestParam String mspId) {
        if (FabricUserRegistration.authenticateUser(username, password, mspId.substring(0, 1).toLowerCase() + mspId.substring(1, mspId.length() - 3))) {
            System.out.println("FabricUserRegistration works");
            CustomUserDetails userDetails = new CustomUserDetails(username, password, mspId, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            // Generate JWT using JwtUtils
            String jwt = jwtUtils.generateTokenFromUserDetails(userDetails);

            return ResponseEntity.ok(jwt);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
        }
    }

    @PostMapping("/submit")
    public String submitTransaction(HttpServletRequest request,
                                    @RequestParam String channelName,
                                    @RequestParam String chaincodeName,
                                    @RequestParam String functionName, @RequestParam String... args){
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
    public String enrollUser(@RequestParam String username, @RequestParam String password) {
        System.out.println("Received");
        if(fabricUserRegistration.addUser(username, password))
            return "User registered successfully";
        else
            return  "User registration failed";
    }
}
