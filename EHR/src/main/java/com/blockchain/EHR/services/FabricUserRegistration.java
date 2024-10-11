package com.blockchain.EHR.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;

import io.grpc.TlsChannelCredentials;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.identity.*;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.blockchain.EHR.config.FabricConfig.readX509CertificateFromPem;

@Service
public class FabricUserRegistration {

    private static final String CA_URL = "https://tlsca.org1.example.com:7054"; // Your CA URL
    private static final String ORG_NAME = "org1.example.com";
    private static final String ADMIN_NAME = "admin";
    private static final String ADMIN_PASSWORD = "adminpw";

    // Path to the CA's TLS certificate
    private static final String CA_CERT_PATH = Paths.get("artifacts", "channel", "crypto-config", "peerOrganizations", "org1.example.com", "tlsca", "tlsca.org1.example.com-cert.pem").toString();

    // Directory where user certificates and keys will be stored
    private static final String WALLET_PATH = "EHR/src/main/resources/static/connection-profiles/org1/wallet";

    public boolean addUser(String username, String password) {
        try {
            // Step 1: Create a CA client for interacting with the CA
            Properties props = new Properties();
            props.put("pemFile", CA_CERT_PATH);
            props.put("allowAllHostNames", "true"); // Not recommended for production

            HFCAClient caClient = HFCAClient.createNewInstance(CA_URL, props);
            caClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

            // Step 2: Enroll the admin user to interact with the CA
            Enrollment adminEnrollment = caClient.enroll(ADMIN_NAME, ADMIN_PASSWORD);
            User admin = new FabricUser(ADMIN_NAME, ORG_NAME, adminEnrollment);

            // Step 3: Register and enroll the new user

            registerAndEnrollUser(caClient, admin, username, password);
            return true;
        } catch (Exception e) {
            e.getMessage();
            return false;
        }
    }

    public static boolean authenticateUser(String username, String password) {
        try {
            // Step 1: Create the CA client
            Properties props = new Properties();
            props.put("pemFile", CA_CERT_PATH); // Path to CA certificate
            props.put("allowAllHostNames", "true"); // Optional for dev environments

            HFCAClient caClient = HFCAClient.createNewInstance(CA_URL, props);
            caClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

            // Step 2: Try enrolling the user with the provided username and password
            Enrollment enrollment = caClient.enroll(username, password);

            // If enrollment succeeds, credentials are valid
            System.out.println("User authenticated successfully: " + username);
            return true;
        } catch (Exception e) {
            // If an exception occurs, it means authentication failed
            System.out.println("Authentication failed for user: " + username);
            return false;
        }
    }

    private static void registerAndEnrollUser(HFCAClient caClient, User admin, String username, String password) throws Exception {
        // Step 1: Register the user with the CA
        RegistrationRequest registrationRequest = new RegistrationRequest(username, "org1.department1");
        registrationRequest.setSecret(password);

        String enrollmentSecret = caClient.register(registrationRequest, admin);
        System.out.println("Successfully registered user: " + username);
        // Step 2: Enroll the registered user to get the enrollment certificate
        Enrollment userEnrollment = caClient.enroll(username, enrollmentSecret);
        System.out.println("Successfully enrolled user: " + username);

        // Save the user's private key and certificate

        saveUserCredentials(username, userEnrollment, "Org1MSP");

    }

    private static void saveUserCredentials(String username, Enrollment enrollment, String mspId) throws Exception {
        // Define the wallet directory
        File walletDir = new File(WALLET_PATH);
        if (!walletDir.exists()) {
            walletDir.mkdirs();
        }

        // Construct the path for the user's JSON wallet entry
        File walletFile = Paths.get(WALLET_PATH, username + ".id").toFile();

        // Convert the private key to PEM format (already PEM encoded)
        String privateKeyPem = Identities.toPemString(enrollment.getKey());

        // Convert the certificate to PEM format
        String certificatePem = enrollment.getCert();

        // Construct the wallet JSON entry for the user
        Map<String, Object> walletJson = new HashMap<>();
        Map<String, String> credentials = new HashMap<>();
        credentials.put("certificate", certificatePem);
        credentials.put("privateKey", privateKeyPem);

        walletJson.put("credentials", credentials);
        walletJson.put("mspId", mspId); // Set the MSP ID
        walletJson.put("type", "X.509");

        // Use Jackson ObjectMapper to serialize the JSON data
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(walletFile, walletJson);

        System.out.println("Saved credentials for user: " + username);
    }

    public Gateway registerUserWithCertificate(String username, String signedCertPem) throws Exception {
        // Convert the PEM string to X509Certificate
        X509Certificate certificate = convertPemToX509Certificate(signedCertPem);

        // Fetch the user's public key from the CA
        PublicKey caPublicKey = fetchUserPublicKeyFromCA(username);

        // Verify the certificate against the fetched public key
        verifyCertificate(certificate, caPublicKey);

        // Create an identity using the verified certificate
        Identity identity = new X509Identity("Org1MSP", certificate);

        // Create the gateway connection using the certificate's identity
        return createGateway(identity);
    }

    private X509Certificate convertPemToX509Certificate(String pem) throws Exception {
        try (ByteArrayInputStream pemStream = new ByteArrayInputStream(pem.getBytes())) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(pemStream);
        }
    }

    private PublicKey fetchUserPublicKeyFromCA(String username) throws Exception {
        // Initialize the CA client
        Properties props = new Properties();
        props.put("pemFile", CA_CERT_PATH);
        props.put("allowAllHostNames", "true");
        HFCAClient caClient = HFCAClient.createNewInstance(CA_URL, props);
        caClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        // Enroll the admin to interact with the CA
        Enrollment adminEnrollment = caClient.enroll(ADMIN_NAME, ADMIN_PASSWORD);
        User admin = new User() {
            @Override
            public String getName() {
                return ADMIN_NAME;
            }

            @Override
            public Set<String> getRoles() {
                return null;
            }

            @Override
            public String getAccount() {
                return null;
            }

            @Override
            public String getAffiliation() {
                return ORG_NAME;
            }

            @Override
            public Enrollment getEnrollment() {
                return adminEnrollment;
            }

            @Override
            public String getMspId() {
                return "Org1MSP";
            }
        };

        // Fetch the user's certificate from the CA
        Enrollment userEnrollment = caClient.reenroll(admin);
        X509Certificate userCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(userEnrollment.getCert().getBytes()));
        return userCert.getPublicKey();
    }

    private void verifyCertificate(X509Certificate certificate, PublicKey caPublicKey) throws Exception {
        certificate.verify(caPublicKey);
    }

    private Gateway createGateway(Identity identity) throws Exception {
        // Set up TLS credentials
        Path tlsCertPath = Paths.get("artifacts", "channel", "crypto-config", "peerOrganizations", "org1.example.com", "tlsca", "tlsca.org1.example.com-cert.pem");
        TlsChannelCredentials tlsCredentials = (TlsChannelCredentials) TlsChannelCredentials.newBuilder()
                .trustManager(tlsCertPath.toFile())
                .build();

        // Create gRPC channel
        ManagedChannel grpcChannel = Grpc.newChannelBuilder("localhost:7051", tlsCredentials)
                .build();

        // Build and connect to Gateway
        return Gateway.newInstance()
                .identity(identity)
                .connection(grpcChannel)
                .connect();
    }

    private static X509Certificate readX509CertificateFromPem(String pem) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        try (var certStream = new java.io.ByteArrayInputStream(pem.getBytes())) {
            return (X509Certificate) factory.generateCertificate(certStream);
        }
    }

    // Utility method to read PrivateKey from PEM string
    private static PrivateKey readPrivateKeyFromPem(String pem) throws Exception {
        return Identities.readPrivateKey(pem);  // Use existing method in Identities
    }

    // Load user identity and private key from JSON wallet
    public static UserCredentials loadUserIdentity(String walletPath, String username) throws Exception {
        // Path to the user's JSON wallet entry
        Path userFilePath = Paths.get(walletPath, username + ".id");

        // Read the user credentials from the JSON file
        ObjectMapper mapper = new ObjectMapper();
        JsonNode userCredentials = mapper.readTree(userFilePath.toFile());

        // Extract certificate and private key from the JSON content
        String certificatePem = userCredentials.path("credentials").path("certificate").asText();
        String privateKeyPem = userCredentials.path("credentials").path("privateKey").asText();

        // Convert PEM to X509 certificate and private key
        X509Certificate certificate = readX509CertificateFromPem(certificatePem);
        PrivateKey privateKey = readPrivateKeyFromPem(privateKeyPem);

        // Manually create X509Identity and return private key separately
        X509Identity identity = new X509Identity("Org1MSP", certificate);  // Replace "Org1MSP" with actual MSP ID
        return new UserCredentials(identity, privateKey);
    }

    // A class to hold both identity and private key
    public static class UserCredentials {
        private final X509Identity identity;
        private final PrivateKey privateKey;

        public UserCredentials(X509Identity identity, PrivateKey privateKey) {
            this.identity = identity;
            this.privateKey = privateKey;
        }

        public X509Identity getIdentity() {
            return identity;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }
    }

    public static Signer createSigner(PrivateKey privateKey) throws Exception {
        return Signers.newPrivateKeySigner(privateKey);
    }

    // Simple implementation of the User interface for Fabric SDK
    public static class FabricUser implements User {
        private String name;
        private String mspId;
        private Enrollment enrollment;

        public FabricUser(String name, String mspId, Enrollment enrollment) {
            this.name = name;
            this.mspId = mspId;
            this.enrollment = enrollment;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Set<String> getRoles() {
            return null;
        }

        @Override
        public String getAccount() {
            return null;
        }

        @Override
        public String getAffiliation() {
            return null;
        }

        @Override
        public Enrollment getEnrollment() {
            return this.enrollment;
        }

        @Override
        public String getMspId() {
            return this.mspId;
        }
    }
}