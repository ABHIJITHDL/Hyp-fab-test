package com.blockchain.EHR.model;

import lombok.Getter;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;

@Getter
public class ContractGatewayPair {
    private final Contract contract;
    private final Gateway gateway;

    public ContractGatewayPair(Contract contract, Gateway gateway) {
        this.contract = contract;
        this.gateway = gateway;
    }

}