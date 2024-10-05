package com.blockchain.EHR.services;

import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
public class FabricService {

    private final Gateway gateway;

    @Autowired
    public FabricService(Gateway gateway) {
        this.gateway = gateway;
    }

    public String submitTransaction(String channelName, String chaincodeName, String functionName, String... args) throws Exception {
        Network network = gateway.getNetwork(channelName);
        Contract contract = network.getContract(chaincodeName);
        byte[] result = contract.submitTransaction(functionName, args);
        System.out.println(Arrays.toString(result));
        return new String(result, StandardCharsets.UTF_8);
    }

    public String evaluateTransaction(String channelName, String chaincodeName, String functionName, String... args) throws Exception {
        Network network = gateway.getNetwork(channelName);
        Contract contract = network.getContract(chaincodeName);
        byte[] result = contract.evaluateTransaction(functionName, args);
        System.out.println(Arrays.toString(result));
        return new String(result, StandardCharsets.UTF_8);
    }


}