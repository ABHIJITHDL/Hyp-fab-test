package com.blockchain.EHR.services;

import com.blockchain.EHR.model.GatewayChannelPair;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class FabricService {

    FabricGatewayService fabricGatewayService;

    public FabricService(FabricGatewayService fabricGatewayService){
        this.fabricGatewayService=fabricGatewayService;
    }


    public String submitTransaction(String channelName, String chaincodeName, String functionName, String[] args,String username,String mspId) {
        GatewayChannelPair gatewayChannelPair = null;
        try {
            gatewayChannelPair = fabricGatewayService.getFabricGateway(username,mspId);
            Gateway gateway = gatewayChannelPair.gateway();
            Network network = gateway.getNetwork(channelName);
            Contract contract = network.getContract(chaincodeName);

            // Submit the transaction
            byte[] result = contract.submitTransaction(functionName, args);
            return new String(result);
        } catch (Exception e) {
            e.printStackTrace();
            return "Transaction failed";
        } finally {
            if (gatewayChannelPair != null) {
                gatewayChannelPair.gateway().close();
                gatewayChannelPair.channel().shutdown();
            }
        }
    }

    public String evaluateTransaction(String channelName, String chaincodeName, String functionName, String[] args,String username,String mspId) {
        GatewayChannelPair gatewayChannelPair = null;
        try {
            gatewayChannelPair = fabricGatewayService.getFabricGateway(username,mspId);
            Gateway gateway = gatewayChannelPair.gateway();
            Network network = gateway.getNetwork(channelName);
            Contract contract = network.getContract(chaincodeName);

            byte[] result = contract.submitTransaction(functionName, args);
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.getMessage();
            return "Transaction failed";
        } finally {
            if (gatewayChannelPair != null) {
                gatewayChannelPair.gateway().close();
                gatewayChannelPair.channel().shutdown();
            }
        }
    }

}