package com.blockchain.EHR.model;

// GatewayChannelPair.java


import io.grpc.ManagedChannel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hyperledger.fabric.client.Gateway;
import org.springframework.stereotype.Service;

@Getter
@Setter
@AllArgsConstructor
public class GatewayChannelPair {
    private final Gateway gateway;
    private final ManagedChannel channel;

}