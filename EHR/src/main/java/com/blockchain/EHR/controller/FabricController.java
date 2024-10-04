package com.blockchain.EHR.controller;

import com.blockchain.EHR.services.FabricService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fabric")
public class FabricController {

    private final FabricService fabricService;

    public FabricController(FabricService fabricService) {
        this.fabricService = fabricService;
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
}
