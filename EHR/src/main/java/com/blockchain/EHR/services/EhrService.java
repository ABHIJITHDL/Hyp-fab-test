package com.blockchain.EHR.services;

import com.blockchain.EHR.model.EhrDocument;
import com.blockchain.EHR.model.Patient;
import com.blockchain.EHR.model.Pending;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@Service
public class EhrService {
    @Autowired
    private com.blockchain.EHR.repository.PatientRepository patientRepository;

    @Autowired
    private com.blockchain.EHR.repository.PendingRepository pendingRepository;

    @Autowired
    private DoctorService doctorService;

    private static final String ALGORITHM = "AES";
    private static final String SECRET_KEY = "MySuperSecretKey";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String encrypt(String data) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private String decrypt(String encryptedData) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }

    public boolean isAccessApproved(String patientId, String doctorId) {
        Pending pendingRequest = pendingRepository.findByPidAndDid(patientId, doctorId);
        return pendingRequest != null && "Accepted".equalsIgnoreCase(pendingRequest.getStatus());
    }

    public void addEhrDocument(String patientId, EhrDocument document) {
        try {
            String ehrJson = objectMapper.writeValueAsString(document); // Convert to JSON
            String encryptedEhr = encrypt(ehrJson); // Encrypt JSON

            Patient patient = new Patient();
            patient.setPatientId(patientId);
            patient.setEhrId(patientId);
            patient.setEhrDocument(encryptedEhr); // Store encrypted data
            patientRepository.save(patient);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public EhrDocument getEhrDocumentForPatient(String patientId, String mspId) {
        Optional<Patient> patientOptional = patientRepository.findById(patientId);
        if (patientOptional.isPresent()) {
            try {
                String encryptedEhr = patientOptional.get().getEhrDocument();
                String decryptedEhrJson = decrypt(encryptedEhr); // Decrypt JSON

                return objectMapper.readValue(decryptedEhrJson, EhrDocument.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public EhrDocument getEhrDocument(String patientId, String did, String mspId) {
        Optional<Patient> patientOptional = patientRepository.findById(patientId);
        if (patientOptional.isPresent()) {
            try {
                String encryptedEhr = patientOptional.get().getEhrDocument();
                String decryptedEhrJson = decrypt(encryptedEhr); // Decrypt JSON

                EhrDocument ehrDocument = objectMapper.readValue(decryptedEhrJson, EhrDocument.class);
                String hash = getHash(ehrDocument);

                // Add access control check
                if (doctorService.addAccess(did, patientId, hash, mspId)) {
                    System.out.println("Add access");
                    return ehrDocument;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean updateEhr(String did, String patientId, String mspId,EhrDocument ehrDocument){
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient != null) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(ehrDocument.toString().getBytes());
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    hexString.append(String.format("%02x", b));
                }
                if (doctorService.addUpdate(did, patientId, hexString.toString(), mspId)) {
                    String ehrJson = objectMapper.writeValueAsString(ehrDocument);
                    String encrypted = encrypt(ehrJson);
                    patient.setEhrDocument(encrypted);
                    patientRepository.save(patient);
                    return true;
                }
            } catch (NoSuchAlgorithmException e) {
                return false;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return false;
    }

    public EhrDocument fetchPdf(String pid) throws Exception {
    Patient patient = patientRepository.findById(pid)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found with ID: " + pid));
    String encryptedEHR = patient.getEhrDocument();

    return objectMapper.readValue(decrypt(encryptedEHR),EhrDocument.class);
    }

    public String getHash(EhrDocument ehrDocument){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ehrDocument.toString().getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {

        }
        return " ";
    }

}
