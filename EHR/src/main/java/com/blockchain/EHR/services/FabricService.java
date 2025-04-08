package com.blockchain.EHR.services;

import com.blockchain.EHR.model.GatewayChannelPair;
import io.grpc.Metadata;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.client.Proposal;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class FabricService {
    private static final Logger logger = LoggerFactory.getLogger(FabricService.class);

    private final FabricGatewayService fabricGatewayService;

    public FabricService(FabricGatewayService fabricGatewayService){
        this.fabricGatewayService=fabricGatewayService;
    }


//    public String submitTransaction(String channelName, String chaincodeName, String functionName, String[] args, String username, String mspId) {
//        GatewayChannelPair gatewayChannelPair = null;
//        try {
//            logger.debug("Fetching Fabric Gateway for user: {}", username);
//            gatewayChannelPair = fabricGatewayService.getFabricGateway(username, mspId);
//            if (gatewayChannelPair == null) {
//                logger.error("Gateway not found for user: {}", username);
//                return "Gateway not found";
//            }
//            Gateway gateway = gatewayChannelPair.gateway();
//            Network network = gateway.getNetwork(channelName);
//            Contract contract = network.getContract(chaincodeName);
//
//            logger.info("Submitting transaction: {} with args: {}", functionName, args);
//            byte[] result = contract.submitTransaction(functionName, args);
//            logger.info("Transaction successful, result: {}", new String(result));
//            return new String(result);
//        }catch (org.hyperledger.fabric.client.EndorseException e) {
//            logger.error("Transaction endorsement failed: {}", e.getMessage());
//            if (e.getDetails() != null && !e.getDetails().isEmpty()) {
//                e.getDetails().forEach(detail -> {
//                    logger.error("Endorsement detail: {}", detail.getMessage());
//                    logger.error("Endorsement detail status: {}", detail.getClass().getName());
//                });
//            } else {
//                logger.error("No endorsement details found in the exception.");
//            }
//            return "Transaction endorsement failed: " + e.getMessage();
//        } catch (io.grpc.StatusRuntimeException e) {
//            logger.error("gRPC error during transaction: {}", e.getStatus().getDescription());
//            logger.error("gRPC status code: {}", e.getStatus().getCode());
//
//            if (e.getTrailers() != null && !e.getTrailers().keys().isEmpty()) {
//                e.getTrailers().keys().forEach(key -> {
//                    Metadata.Key<String> metadataKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
//                    String value = e.getTrailers().get(metadataKey);
//                    logger.error("gRPC trailer - {}: {}", key, value);
//                });
//            } else {
//                logger.error("No trailers (attached details) found in the gRPC exception.");
//            }
//            return "gRPC error: " + e.getStatus().getDescription();
//        } catch (Exception e) {
//            logger.error("Unexpected error during transaction submission", e);
//            return "Transaction failed: " + e.getMessage();
//        } finally {
//            if (gatewayChannelPair != null) {
//                gatewayChannelPair.gateway().close();
//                gatewayChannelPair.channel().shutdown();
//            }
//        }
//    }

    public String submitTransaction(String channelName, String chaincodeName, String functionName, String[] args, String username, String mspId) {
        GatewayChannelPair gatewayChannelPair = null;
        try {
            logger.debug("Fetching contract for channel: {}, chaincode: {}, user: {}", channelName, chaincodeName, username);
            gatewayChannelPair = fabricGatewayService.getFabricGateway(username, mspId);
            if (gatewayChannelPair == null) {
                logger.error("Gateway not found for user: {}", username);
                return "Gateway not found";
            }
            Gateway gateway = gatewayChannelPair.gateway();
            Network network = gateway.getNetwork(channelName);
            Contract contract = network.getContract(chaincodeName);

            logger.info("Building proposal for function: {} with args: {}", functionName, args);
            Proposal proposal = contract.newProposal(functionName)
                    .addArguments(args)
                    .build();

            logger.info("Endorsing and submitting transaction...");
            byte[] result = proposal.endorse().submit();

            String resultString = new String(result, StandardCharsets.UTF_8);
            logger.info("Transaction submitted successfully. Result: {}", resultString);
            return resultString;
        } catch (org.hyperledger.fabric.client.EndorseException e) {
            logger.error("Transaction endorsement failed: {}", e.getMessage());
            if (e.getDetails() != null && !e.getDetails().isEmpty()) {
                e.getDetails().forEach(detail -> {
                    logger.error("Endorsement detail: {}", detail.getMessage());
                    logger.error("Endorsement detail status: {}", detail.getClass().getName());
                });
            } else {
                logger.error("No endorsement details found in the exception.");
            }
            return "Transaction endorsement failed: " + e.getMessage();
        } catch (io.grpc.StatusRuntimeException e) {
            logger.error("gRPC error during transaction: {}", e.getStatus().getDescription());
            logger.error("gRPC status code: {}", e.getStatus().getCode());

            if (e.getTrailers() != null && !e.getTrailers().keys().isEmpty()) {
                e.getTrailers().keys().forEach(key -> {
                    Metadata.Key<String> metadataKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                    String value = e.getTrailers().get(metadataKey);
                    logger.error("gRPC trailer - {}: {}", key, value);
                });
            } else {
                logger.error("No trailers (attached details) found in the gRPC exception.");
            }
            return "gRPC error: " + e.getStatus().getDescription();
        } catch (Exception e) {
            logger.error("Unexpected error during transaction submission", e);
            return "Transaction failed: " + e.getMessage();
        }finally {
            if (gatewayChannelPair != null) {
                try {
                    gatewayChannelPair.gateway().close();
                } catch (Exception e) {
                    logger.error("Error closing Gateway: {}", e.getMessage());
                }
                try {
                    gatewayChannelPair.channel().shutdown();
                } catch (Exception e) {
                    logger.error("Error shutting down ManagedChannel: {}", e.getMessage());
                }
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
            e.printStackTrace();
            return "Transaction failed";
        } finally {
            if (gatewayChannelPair != null) {
                gatewayChannelPair.gateway().close();
                gatewayChannelPair.channel().shutdown();
            }
        }
    }

    public Contract getContract(String channelName, String chaincodeName, String username, String mspId) throws Exception {
        GatewayChannelPair gatewayChannelPair = fabricGatewayService.getFabricGateway(username,mspId);
        if (gatewayChannelPair == null) {
            throw new RuntimeException("Gateway not found for user: " + username);
        }
        Gateway gateway = gatewayChannelPair.gateway();
        Network network = gateway.getNetwork(channelName);
        return network.getContract(chaincodeName);
    }

}