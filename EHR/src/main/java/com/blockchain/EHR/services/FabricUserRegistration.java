package com.blockchain.EHR.services;

import org.apache.http.HttpStatus;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.*;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Properties;
import java.util.Set;
import java.util.Base64;

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
            String username = "user11";
            String password = "user11pw";
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
        java.nio.file.Files.write(certFile.toPath(), enrollment.getCert().getBytes());

        // Write the private key to the file in PEM format
        try (PemWriter pemWriter = new PemWriter(new FileWriter(keyFile))) {
            PemObject pemObject = new PemObject("PRIVATE KEY", enrollment.getKey().getEncoded());
            pemWriter.writeObject(pemObject);
        }

        System.out.println("Saved credentials for user: " + username);
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