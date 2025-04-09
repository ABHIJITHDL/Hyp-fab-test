package com.blockchain.EHR.services;

import com.blockchain.EHR.model.GatewayChannelPair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Locale;


@Service
public class FabricGatewayService {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    GatewayChannelPair getFabricGateway(String username, String mspId) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        // Extract organization name from mspId (e.g., Org1MSP -> org1)
        String orgName = mspId.substring(0, mspId.length() - 3).toLowerCase();

        // Load connection profile dynamically for the organization
        ClassPathResource connectionProfileResource = new ClassPathResource("static/connection-profiles/" + orgName + "/connection-" + orgName + ".json");
        File connectionProfileFile = connectionProfileResource.getFile();
        JsonNode connectionProfile = mapper.readTree(connectionProfileFile);

        // Retrieve peer information dynamically based on organization and peer0
        String peerName = connectionProfile.path("organizations").path(mspId.substring(0, mspId.length() - 3)).path("peers").get(0).asText();
        String grpcUrl = connectionProfile.path("peers").path(peerName).path("url").asText();
        String tlsCertPem = connectionProfile.path("peers").path(peerName).path("tlsCACerts").path("pem").asText();

        ClassPathResource walletResource = new ClassPathResource("static/connection-profiles/" + orgName + "/wallet");
        Path walletPath = walletResource.getFile().toPath();
        Path userFilePath = walletPath.resolve(username + ".id");
        JsonNode userCredentials = mapper.readTree(userFilePath.toFile());

        String certificatePem = userCredentials.path("credentials").path("certificate").asText();
        String privateKeyPem = userCredentials.path("credentials").path("privateKey").asText();

        // Write TLS certificate to a temporary file
        Path tlsCertPath = Files.createTempFile("tlsCert", ".pem");
        Files.writeString(tlsCertPath, tlsCertPem);

        // Create Identity and Signer
        X509Certificate certificate = readX509CertificateFromPem(certificatePem);
        Identity identity = new X509Identity(mspId, certificate);
        PrivateKey privateKey = getPrivateKeyFromPem(privateKeyPem);
        Signer signer = Signers.newPrivateKeySigner(privateKey);
        System.out.println("Signer created");
        // Set up TLS credentials
        ChannelCredentials tlsCredentials = TlsChannelCredentials.newBuilder()
                .trustManager(tlsCertPath.toFile())
                .build();

        // Create gRPC channel using dynamic URL from connection profile
        ManagedChannel grpcChannel = Grpc.newChannelBuilder(grpcUrl.substring(8), tlsCredentials)
                .build();

        // Create gateway connection
        Gateway gateway = Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .connection(grpcChannel)
                .connect();

        return new GatewayChannelPair(gateway, grpcChannel);
    }


    public PrivateKey getPrivateKeyFromPem(String pemContent) throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(pemContent))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            return converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
        }
    }

    public X509Certificate readX509CertificateFromPem(String certString) throws Exception {
        // Remove the "BEGIN CERTIFICATE" and "END CERTIFICATE" headers/footers
        String cleanCert = certString.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");  // Remove all whitespace/newlines

        // Decode the base64 certificate
        byte[] decodedCert = Base64.getDecoder().decode(cleanCert);

        // Create a CertificateFactory
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        // Convert the decoded byte array into an X509Certificate object

        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(decodedCert));
    }
}
