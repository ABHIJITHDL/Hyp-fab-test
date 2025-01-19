package com.blockchain.EHR.services;

import com.blockchain.EHR.model.EhrDocument;
import com.blockchain.EHR.model.Patient;
import com.blockchain.EHR.model.Pending;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
public class EhrService {
    @Autowired
    private com.blockchain.EHR.repository.PatientRepository patientRepository;

    @Autowired
    private com.blockchain.EHR.repository.PendingRepository pendingRepository;

    @Autowired
    private DoctorService doctorService;

    public boolean isAccessApproved(String patientId, String doctorId) {
        Pending pendingRequest = pendingRepository.findByPidAndDid(patientId, doctorId);
        return pendingRequest != null && "Accepted".equalsIgnoreCase(pendingRequest.getStatus());
    }

    public EhrDocument getEhrDocument(String patientId, String did, String mspId) {
        Optional<Patient> patientOptional = patientRepository.findById(patientId);
        if (patientOptional.isPresent()) {
            EhrDocument ehrDocument = patientOptional.get().getEhrDocument();
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(ehrDocument.toString().getBytes());
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    hexString.append(String.format("%02x", b));
                }
                if (doctorService.addAccess(did, patientId, hexString.toString(), mspId)) {
                    return ehrDocument;
                }
            } catch (NoSuchAlgorithmException e) {
                return null;
            }
        }
        return null;
    }
    public void addEhrDocument(String patientId,EhrDocument document){
        Patient patient = new Patient();
        patient.setPatientId(patientId);
        patient.setEhrId(patientId);
        patient.setEhrDocument(document);
        patientRepository.save(patient);
    }

    public boolean updateEhrDocument(String patientId, EhrDocument updatedEhrDocument) {
        Optional<Patient> patientOptional = patientRepository.findById(patientId);
        if (patientOptional.isPresent()) {
            Patient patient = patientOptional.get();
            patient.setEhrDocument(updatedEhrDocument);
            patientRepository.save(patient);
            return true;
        }
        return false;
    }

    public EhrDocument fetchPdf(String pid) {
    Patient patient = patientRepository.findById(pid)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found with ID: " + pid));
    return patient.getEhrDocument();
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
