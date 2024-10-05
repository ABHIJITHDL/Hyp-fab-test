package com.blockchain.EHR.services;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Properties;
import java.util.Set;

public class FabricUserRegistration {

    private static final String CA_URL = "https://tlsca.org1.example.com:7054"; // Your CA URL
    private static final String ORG_NAME = "org1.example.com";
    private static final String ADMIN_NAME = "admin";
    private static final String ADMIN_PASSWORD = "adminpw";

    // Path to the CA's TLS certificate
    private static final String CA_CERT_PATH = Paths.get("artifacts", "channel", "crypto-config", "peerOrganizations", "org1.example.com", "tlsca", "tlsca.org1.example.com-cert.pem").toString();

    // Directory where user certificates and keys will be stored
    private static final String WALLET_PATH = Paths.get("EHR","src", "main", "resources", "static", "wallet").toString();

    public static void main(String[] args) {
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
            String username = "user9";
            String password = "user9pw";
            registerAndEnrollUser(caClient, admin, username, password);

        } catch (Exception e) {
            e.printStackTrace();
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

        // Save the user’s private key and certificate
        saveUserCredentials(username, userEnrollment);
    }

    private static void saveUserCredentials(String username, Enrollment enrollment) throws Exception {
        // Save the user's certificate and private key to a directory for future use
        File walletDir = new File(WALLET_PATH);
        if (!walletDir.exists()) {
            walletDir.mkdirs(); // Use mkdirs() to create parent directories if needed
        }

        File certFile = Paths.get(WALLET_PATH, username + "-cert.pem").toFile();
        File keyFile = Paths.get(WALLET_PATH, username + "-priv-key.pem").toFile();

        // Write the certificate to the file
        java.nio.file.Files.write(certFile.toPath(), enrollment.getCert().getBytes());

        // Write the private key to the file in PEM format
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter(keyFile))) {
            PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(enrollment.getKey().getEncoded());
            pemWriter.writeObject(privateKeyInfo);
        } catch (Exception e) {
            e.printStackTrace();
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
