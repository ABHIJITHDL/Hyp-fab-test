package com.blockchain.EHR.services;

import com.blockchain.EHR.Repository.PatientRepository;
import com.blockchain.EHR.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;

@Service
public class PdfService {

    @Autowired
    private PatientRepository patientRepository;


    public Patient upload(String pid, String eid, MultipartFile pdf) throws IOException {
        Patient patient=new Patient();
        patient.setPatientId(pid);
        patient.setEhrId(eid);
        patient.setPdfData(pdf.getBytes());
        return patientRepository.save(patient);
    }
}
