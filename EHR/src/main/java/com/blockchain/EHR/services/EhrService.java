package com.blockchain.EHR.services;

import com.blockchain.EHR.model.EhrDocument;
import com.blockchain.EHR.model.Patient;
import com.blockchain.EHR.model.Pending;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EhrService {
    @Autowired
    private com.blockchain.EHR.repository.PatientRepository patientRepository;

    @Autowired
    private com.blockchain.EHR.repository.PendingRepository pendingRepository;

    public boolean isAccessApproved(String patientId, String doctorId) {
        Pending pendingRequest = pendingRepository.findByPidAndDid(patientId, doctorId);
        return pendingRequest != null && "Accepted".equalsIgnoreCase(pendingRequest.getStatus());
    }

    public Optional<EhrDocument> getEhrDocument(String patientId) {
        return patientRepository.findById(patientId).map(Patient::getEhrDocument);
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
}
