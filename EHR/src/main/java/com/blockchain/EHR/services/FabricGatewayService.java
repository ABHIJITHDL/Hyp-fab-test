package com.blockchain.EHR.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import io.grpc.netty.NettyChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.identity.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.blockchain.EHR.config.FabricConfig.getPrivateKeyFromPem;
import static com.blockchain.EHR.config.FabricConfig.readX509CertificateFromPem;

@Service
public class FabricGatewayService {

    private static final String WALLET_PATH = "path/to/wallet"; // Update with your wallet path

    public Gateway fabricGateway(String username) throws Exception {
        System.out.println("user "+username+" invoked transaction");
        ObjectMapper mapper = new ObjectMapper();
        ClassPathResource connectionProfileResource = new ClassPathResource("static/connection-profiles/org1/connection-org1.json");
        File connectionProfileFile = connectionProfileResource.getFile();
        JsonNode connectionProfile = mapper.readTree(connectionProfileFile);

        // Extract values from connection profile
        String mspId = connectionProfile.path("organizations").path("Org1").path("mspid").asText();
        String tlsCertPem = connectionProfile.path("peers").path("peer0.org1.example.com").path("tlsCACerts").path("pem").asText();

        // Read the user credentials from the JSON file
        Path userFilePath = Paths.get(WALLET_PATH, username + ".id");
        JsonNode userCredentials = mapper.readTree(userFilePath.toFile());

        String certificatePem = userCredentials.path("credentials").path("certificate").asText();
        String privateKeyPem = userCredentials.path("credentials").path("privateKey").asText();

        // Write the TLS certificate to a temporary file
        Path tlsCertPath = Files.createTempFile("tlsCert", ".pem");
        Files.writeString(tlsCertPath, tlsCertPem);

        // Read certificate and extract public key
        X509Certificate certificate = readX509CertificateFromPem(certificatePem);
        Identity identity = new X509Identity(mspId, certificate);

        // Read private key
        PrivateKey privateKey = getPrivateKeyFromPem(privateKeyPem);
        Signer signer = Signers.newPrivateKeySigner(privateKey);

        // Set up TLS credentials
        ChannelCredentials tlsCredentials = TlsChannelCredentials.newBuilder()
                .trustManager(tlsCertPath.toFile())
                .build();

        // Create gRPC channel
        ManagedChannel grpcChannel = Grpc.newChannelBuilder("localhost:7051", tlsCredentials)
                .build();

        // Build and connect to Gateway
        return Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .connection(grpcChannel)
                .connect();
    }
}
