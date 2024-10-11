package com.blockchain.EHR.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
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

@Configuration
@Slf4j
public class FabricConfig {
    private static final Logger logger = Logger.getLogger(FabricConfig.class.getName());
    private static final String WALLET_PATH = "EHR/src/main/resources/static/connection-profiles/org1/wallet";

    @Bean
    public Gateway fabricGateway() throws Exception {
        String username = "user9";
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

    static {
        Security.addProvider(new BouncyCastleProvider());
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

    public static boolean verifySignature(String data, String signature, String publicKeyContent) throws Exception {
        logger.info("Verifying signature for data: " + data);

        // Load the public key from the PEM content
        PublicKey publicKey = getPublicKeyFromPem(publicKeyContent);

        // Initialize the Signature object with ECDSA and SHA-256
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initVerify(publicKey);
        sig.update(data.getBytes(StandardCharsets.UTF_8)); // Ensure the same encoding as used in the signing process

        // Decode the base64-encoded signature
        byte[] signatureBytes = java.util.Base64.getDecoder().decode(signature);

        // Verify the signature against the data
        boolean isValid = sig.verify(signatureBytes);
        logger.info("Signature verification result: " + isValid);
        return isValid;
    }

    private static PublicKey getPublicKeyFromPem(String pemContent) throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(pemContent))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            // Check if the object is a certificate and extract the public key
            if (object instanceof X509CertificateHolder certificateHolder) {
                return converter.getPublicKey(certificateHolder.getSubjectPublicKeyInfo());
            } else if (object instanceof SubjectPublicKeyInfo publicKeyInfo) {
                // Handle if it's directly a public key
                return converter.getPublicKey(publicKeyInfo);
            } else {
                throw new IllegalArgumentException("Provided PEM content is neither a valid certificate nor a public key");
            }
        }
    }
    public static Signer getSignerFromPrivateKeyPem(String privateKeyPem) throws Exception {
        PrivateKey privateKey = getPrivateKeyFromPem(privateKeyPem);
        return Signers.newPrivateKeySigner(privateKey);
    }

    public static PrivateKey getPrivateKeyFromPem(String pemContent) throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(pemContent))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            return converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
        }
    }

    public static X509Certificate readX509CertificateFromPem(String certString) throws Exception {
        // Remove the "BEGIN CERTIFICATE" and "END CERTIFICATE" headers/footers
        String cleanCert = certString.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");  // Remove all whitespace/newlines

        // Decode the base64 certificate
        byte[] decodedCert = Base64.getDecoder().decode(cleanCert);

        // Create a CertificateFactory
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        // Convert the decoded byte array into an X509Certificate object
        X509Certificate cert = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(decodedCert));

        return cert;
    }

    public static PublicKey getPublicKeyFromPrivateKeyPem(String privateKeyPem) throws Exception {
        PrivateKey privateKey = getPrivateKeyFromPem(privateKeyPem);
        // Assuming the private key is an RSA key, derive the public key
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        java.security.spec.RSAPrivateKeySpec privateKeySpec = keyFactory.getKeySpec(privateKey, java.security.spec.RSAPrivateKeySpec.class);
        java.security.spec.RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(privateKeySpec.getModulus(), java.math.BigInteger.valueOf(65537));
        return keyFactory.generatePublic(publicKeySpec);
    }
}

