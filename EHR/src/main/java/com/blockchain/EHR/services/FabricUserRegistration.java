package com.blockchain.EHR.services;

import io.grpc.Grpc;
import io.grpc.ManagedChannel;

import io.grpc.TlsChannelCredentials;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.Set;

@Service
public class FabricUserRegistration {

    private static final String CA_URL = "https://tlsca.org1.example.com:7054"; // Your CA URL
    private static final String ORG_NAME = "org1.example.com";
    private static final String ADMIN_NAME = "admin";
    private static final String ADMIN_PASSWORD = "adminpw";

    // Path to the CA's TLS certificate
    private static final String CA_CERT_PATH = Paths.get("artifacts", "channel", "crypto-config", "peerOrganizations", "org1.example.com", "tlsca", "tlsca.org1.example.com-cert.pem").toString();

    // Directory where user certificates and keys will be stored
    private static final String WALLET_PATH = Paths.get("EHR", "src", "main", "resources", "static", "wallet").toString();

    public static void addUser(String username, String password) {
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

        } catch (Exception e) {
            e.printStackTrace();
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
        saveUserCredentials(username, userEnrollment);
    }

    private static void saveUserCredentials(String username, Enrollment enrollment) throws Exception {
        File walletDir = new File(WALLET_PATH);
        if (!walletDir.exists()) {
            walletDir.mkdirs();
        }

        File certFile = Paths.get(WALLET_PATH, username + "-cert.pem").toFile();
        File keyFile = Paths.get(WALLET_PATH, username + "-priv-key.pem").toFile();

        // Write the certificate to the file
        Files.write(certFile.toPath(), enrollment.getCert().getBytes());

        // Write the private key to the file in PEM format
        try (PemWriter pemWriter = new PemWriter(new FileWriter(keyFile))) {
            PemObject pemObject = new PemObject("PRIVATE KEY", enrollment.getKey().getEncoded());
            pemWriter.writeObject(pemObject);
        }

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