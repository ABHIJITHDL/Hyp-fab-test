package com.blockchain.EHR.controller;

import com.blockchain.EHR.services.FabricService;
import com.blockchain.EHR.services.FabricUserRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fabric")
public class FabricController {

    private final FabricService fabricService;
    private final FabricUserRegistration fabricUserRegistration;

    @Autowired
    public FabricController(FabricService fabricService, FabricUserRegistration fabricUserRegistration) {
        this.fabricService = fabricService;
        this.fabricUserRegistration = fabricUserRegistration;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        if (FabricUserRegistration.authenticateUser(username, password)) {
            return ResponseEntity.ok("Authentication successful");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
        }
    }

    @PostMapping("/submit")
    public String submitTransaction(@RequestParam String channelName,
                                    @RequestParam String chaincodeName,
                                    @RequestParam String functionName,
                                    @RequestParam String username, @RequestParam String... args){
        return fabricService.submitTransaction(channelName, chaincodeName, functionName, args,username);
    }

    @GetMapping("/query")
    public String queryTransaction(@RequestParam String channelName,
                                    @RequestParam String chaincodeName,
                                    @RequestParam String functionName,
                                    @RequestParam String username, @RequestParam String... args){
        return fabricService.evaluateTransaction(channelName, chaincodeName, functionName, args,username);
    }


    @PostMapping("/enrollAdmin")
    public String enrollAdmin() {
        return "Admin enrolled successfully";
    }


    @PostMapping("/register")
    public String enrollUser(@RequestParam String username, @RequestParam String password) {
        fabricUserRegistration.addUser(username, password);
        return "User enrolled successfully";
    }
}
