package com.blockchain.EHR.config;


import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.identity.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Properties;

@Configuration
public class FabricConfig {

    @Bean
    public Gateway fabricGateway() throws Exception {
        // Paths to the certificate and private key
        Path certPath = Paths.get("D:\\Applicaitons\\BasicNetwork-2.0\\Ehr_Nodejs\\connection-profiles\\org1\\wallet\\user7-cert.pem");
        Path keyPath = Paths.get("D:\\Applicaitons\\BasicNetwork-2.0\\Ehr_Nodejs\\connection-profiles\\org1\\wallet\\user7-priv-key.pem");
        Path tlsCertPath = Paths.get("D:/Applicaitons/BasicNetwork-2.0/artifacts/channel/crypto-config/peerOrganizations/org1.example.com/tlsca/tlsca.org1.example.com-cert.pem");

        // Read certificate
        X509Certificate certificate = readX509Certificate(certPath);
        Identity identity = new X509Identity("Org1MSP", certificate);

        // Read private key
        PrivateKey privateKey = getPrivateKey(keyPath);
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

    private X509Certificate readX509Certificate(Path certificatePath) throws Exception {
        try (var certInputStream = Files.newInputStream(certificatePath)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(certInputStream);
        }
    }

    private PrivateKey getPrivateKey(Path privateKeyPath) throws Exception {
        try (var keyInputStream = Files.newInputStream(privateKeyPath)) {
            return Identities.readPrivateKey(new String(keyInputStream.readAllBytes()));
        }
    }
}