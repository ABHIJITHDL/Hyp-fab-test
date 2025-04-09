package com.blockchain.EHR.services;

import com.blockchain.EHR.model.FabricUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.Attribute;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAIdentity;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
public class FabricUserRegistration {

    private static final String ADMIN_NAME = "admin";
    private static final String ADMIN_PASSWORD = "adminpw";

//    // Path to the CA's TLS certificate
//    private static final String CA_CERT_PATH = Paths.get("artifacts", "channel", "crypto-config", "peerOrganizations", "org1.example.com", "tlsca", "tlsca.org1.example.com-cert.pem").toString();
//
//    // Directory where user certificates and keys will be stored
//    private static final String WALLET_PATH = "EHR/src/main/resources/static/connection-profiles/org1/wallet";

    public boolean addUser(String username, String password,String mspId) {
        try {
            String organization = getOrganizationFromMSP(mspId);
            Map<String, String> caConfig = getCAConfig(organization);
            String CA_CERT_CONTENT = caConfig.get("CA_CERT_CONTENT");
            String CA_URL = caConfig.get("CA_URL");
            Properties props = new Properties();
            props.put("pemBytes", CA_CERT_CONTENT.getBytes());
            props.put("allowAllHostNames", "true"); // Not recommended for production

            HFCAClient caClient = HFCAClient.createNewInstance(CA_URL, props);
            caClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            // Step 2: Enroll the admin user to interact with the CA
            Enrollment adminEnrollment = caClient.enroll(ADMIN_NAME, ADMIN_PASSWORD);
            User admin = new FabricUser(ADMIN_NAME, getOrganizationFromMSP(mspId), adminEnrollment);
            // Step 3: Register and enroll the new user

            registerAndEnrollUser(caClient, admin, username, password,mspId);
            System.out.println("User "+ username+ " Registered");
            return true;
        } catch (Exception e) {
            e.getMessage();
            return false;
        }
    }

    public String getOrganizationFromMSP(String mspId){
        return mspId.substring(0, 1).toLowerCase() + mspId.substring(1, mspId.length() - 3);
    }

    public boolean authenticateUser(String username, String password, String msp) {
        try {
            String organization = getOrganizationFromMSP(msp);
            System.out.println(organization);
            Map<String, String> caConfig = getCAConfig(organization);
            String CA_CERT_CONTENT = caConfig.get("CA_CERT_CONTENT");
            String CA_URL = caConfig.get("CA_URL");

            // Step 1: Create the CA client
            Properties props = new Properties();
            props.put("pemBytes", CA_CERT_CONTENT.getBytes());
            // Use the temporary file path
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
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static Map<String, String> getCAConfig(String organization) {
        Map<String, String> caConfig = new HashMap<>();
        try {
            // Load the JSON configuration file
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource connectionProfileResource = new ClassPathResource(String.format("static/connection-profiles/%s/connection-%s.json", organization.toLowerCase(), organization.toLowerCase()));
            File connectionProfileFile = connectionProfileResource.getFile();
            JsonNode connectionProfile = mapper.readTree(connectionProfileFile);

            // Retrieve the configuration for the specified organization
            JsonNode orgConfig = connectionProfile.path("organizations").path(organization.substring(0, 1).toUpperCase() + organization.substring(1));
            if (!orgConfig.isMissingNode()) {
                String caName = orgConfig.path("certificateAuthorities").get(0).asText();
                JsonNode caConfigNode = connectionProfile.path("certificateAuthorities").path(caName);
                caConfig.put("CA_URL", caConfigNode.path("url").asText());

                // Load the certificate of peer0
                JsonNode peerConfigNode = connectionProfile.path("peers").path("peer0."+organization+".example.com");
                caConfig.put("CA_CERT_CONTENT", peerConfigNode.path("tlsCACerts").path("pem").asText());
            } else {
                throw new IllegalArgumentException("Organization not found in configuration: " + organization);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to read CA configuration", e);
        }
        return caConfig;
    }
    private  void registerAndEnrollUser(HFCAClient caClient, User admin, String username, String password,String mspId) throws Exception {
        // Step 1: Register the user with the CA
        RegistrationRequest registrationRequest = new RegistrationRequest(username);
        registrationRequest.setSecret(password);
        System.out.println("Registration request created");
        String enrollmentSecret = caClient.register(registrationRequest, admin);
        System.out.println("Successfully registered user: " + username);
        // Step 2: Enroll the registered user to get the enrollment certificate
        Enrollment userEnrollment = caClient.enroll(username, enrollmentSecret);
        System.out.println("Successfully enrolled user: " + username);

        // Save the user's private key and certificate

        saveUserCredentials(username, userEnrollment, mspId);

    }

    private  void saveUserCredentials(String username, Enrollment enrollment, String mspId) throws Exception {
        String org = getOrganizationFromMSP(mspId);
        // Define the wallet directory
        ClassPathResource walletResource = new ClassPathResource("static/connection-profiles/" + org + "/wallet");
        File walletDir = walletResource.getFile();

        // Construct the path for the user's JSON wallet entry
        File walletFile = new File(walletDir, username + ".id");


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

    public static String getRole(HFCAClient caClient, String username) throws Exception {
        HFCAIdentity identity = caClient.newHFCAIdentity(username);
        identity.read(null);
        return identity.getAttributes().stream()
                .filter(attr -> "role".equals(attr.getName()))
                .map(Attribute::getValue)
                .findFirst()
                .orElse("ROLE_USER"); // Default role if not found
    }

}