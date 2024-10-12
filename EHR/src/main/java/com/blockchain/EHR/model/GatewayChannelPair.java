package com.blockchain.EHR.model;

import io.grpc.ManagedChannel;
import org.hyperledger.fabric.client.Gateway;


public record GatewayChannelPair(Gateway gateway, ManagedChannel channel) {
}