package com.blockchain.EHR.controller;

import com.blockchain.EHR.services.FabricCAService;
import com.blockchain.EHR.services.FabricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fabric")
public class FabricController {

    private final FabricService fabricService;

    private final FabricCAService fabricCAService;

    public FabricController(FabricService fabricService,FabricCAService fabricCAService) {
        this.fabricService = fabricService;
        this.fabricCAService=fabricCAService;
    }

    @PostMapping("/submit")
    public String submitTransaction(@RequestParam String channelName,
                                    @RequestParam String chaincodeName,
                                    @RequestParam String functionName,
                                    @RequestParam String... args) throws Exception {
        return fabricService.submitTransaction(channelName, chaincodeName, functionName, args);
    }

    @GetMapping("/query")
    public String queryTransaction(@RequestParam String channelName,
                                   @RequestParam String chaincodeName,
                                   @RequestParam String functionName,
                                   @RequestParam String... args) throws Exception {
        return fabricService.evaluateTransaction(channelName, chaincodeName, functionName, args);
    }


    @PostMapping("/enrollAdmin")
    public String enrollAdmin() {
        fabricCAService.enrollAdmin();
        return "Admin enrolled successfully";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username, @RequestParam String password) {
        fabricCAService.registerUser(username, password);
        return "User registered successfully";
    }

    @PostMapping("/enroll")
    public String enrollUser(@RequestParam String username, @RequestParam String password) {
        fabricCAService.enrollUser(username, password);
        return "User enrolled successfully";
    }
}
