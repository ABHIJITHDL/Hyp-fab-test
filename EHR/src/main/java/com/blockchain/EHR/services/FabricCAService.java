package com.blockchain.EHR.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
public class FabricCAService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String CA_URL = "https://tlsca.org1.example.com:7054";
    private static final String TLS_CERT_PATH = "D:\\Applicaitons\\BasicNetwork-2.0\\artifacts\\channel\\crypto-config\\peerOrganizations\\org1.example.com\\tlsca\\tlsca.org1.example.com-cert.pem";

    public void registerUser(String username, String password) {
        String url = CA_URL + "/register";
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("admin", "adminpw");
        headers.set("Content-Type", "application/json");

        String body = String.format("{\"id\":\"%s\",\"type\":\"client\",\"secret\":\"%s\",\"affiliation\":\"\"}", username, password);

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        System.out.println("Register Response: " + response.getBody());
    }

    public void enrollUser(String username, String password) {
        String url = CA_URL + "/enroll";
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        headers.set("Content-Type", "application/json");

        String body = String.format("{\"hosts\":[\"DESKTOP-2GTJ186\"],\"certificate_request\":\"<CSR_CONTENT>\",\"profile\":\"\",\"crl_override\":\"\",\"label\":\"\",\"NotBefore\":\"0001-01-01T00:00:00Z\",\"NotAfter\":\"0001-01-01T00:00:00Z\",\"CAName\":\"ca.org1.example.com\"}");

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        System.out.println("Enroll Response: " + response.getBody());
    }

    public void enrollAdmin() {
        String url = CA_URL + "/enroll";
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("admin", "adminpw");
        headers.set("Content-Type", "application/json");

        String body = String.format("{\"hosts\":[\"DESKTOP-2GTJ186\"],\"certificate_request\":\"<CSR_CONTENT>\",\"profile\":\"\",\"crl_override\":\"\",\"label\":\"\",\"NotBefore\":\"0001-01-01T00:00:00Z\",\"NotAfter\":\"0001-01-01T00:00:00Z\",\"CAName\":\"ca.org1.example.com\"}");

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        System.out.println("Enroll Admin Response: " + response.getBody());
    }

    private RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        return factory;
    }
}