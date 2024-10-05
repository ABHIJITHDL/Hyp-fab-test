package com.blockchain.EHR.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FabricCAService {
    private static final Logger logger = LoggerFactory.getLogger(FabricCAService.class);

    @Autowired
    private RestTemplate restTemplate;

    private static final String CA_URL = "https://tlsca.org1.example.com:7054";

    public void registerUser(String username, String password) {
        String url = CA_URL + "/register";
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("admin", "adminpw");
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"id\":\"%s\",\"type\":\"client\",\"secret\":\"%s\",\"affiliation\":\"\"}", username, password);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            logger.info("Sending registration request to URL: {}", url);
            logger.debug("Request headers: {}", headers);
            logger.debug("Request body: {}", body);

            // Perform the exchange (POST request)
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            // Process the response
            logger.info("Register Response: {}", response.getBody());
        } catch (RestClientException e) {
            // Handle any errors
            logger.error("Error during registration: {}", e.getMessage(), e);
        }
    }

    public void enrollUser(String username, String password) {
        String url = CA_URL + "/enroll";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password); // Basic authentication for the CA server
        headers.setContentType(MediaType.APPLICATION_JSON); // Set content type as JSON

        // Prepare the certificate signing request (CSR) content
        String body = "{"
                + "\"hosts\": [\"localhost\"],"
                + "\"certificate_request\": \"<CSR_CONTENT>\","
                + "\"profile\": \"\","
                + "\"crl_override\": \"\","
                + "\"label\": \"\","
                + "\"NotBefore\": \"0001-01-01T00:00:00Z\","
                + "\"NotAfter\": \"0001-01-01T00:00:00Z\","
                + "\"CAName\": \"ca.org1.example.com\""
                + "}";

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            logger.info("Sending enrollment request to URL: {}", url);
            logger.debug("Request headers: {}", headers);
            logger.debug("Request body: {}", body);

            // Perform the exchange (POST request)
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            // Process the response
            logger.info("Enroll Response: {}", response.getBody());
        } catch (RestClientException e) {
            // Handle any errors
            logger.error("Error during enrollment: {}", e.getMessage(), e);
        }
    }


    public void enrollAdmin() {
        String url = CA_URL + "/enroll";
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("admin", "adminpw");
        headers.set("Content-Type", "application/json");

        String body = "{\"hosts\":[\"DESKTOP-2GTJ186\"],\"certificate_request\":\"<CSR_CONTENT>\",\"profile\":\"\",\"crl_override\":\"\",\"label\":\"\",\"NotBefore\":\"0001-01-01T00:00:00Z\",\"NotAfter\":\"0001-01-01T00:00:00Z\",\"CAName\":\"ca.org1.example.com\"}";

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        System.out.println("Enroll Admin Response: " + response.getBody());
    }
}