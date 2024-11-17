package com.blockchain.EHR.services;


import com.blockchain.EHR.model.Patient;
import com.blockchain.EHR.repository.PatientRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class DocxService {

    @Autowired
    private PatientRepository patientRepository;

    // Upload DOCX file and store it in the database
    public Patient uploadDocx(String pid, String eid, MultipartFile docxFile) throws IOException {
        byte[] docxData = docxFile.getBytes();

        Patient patient = new Patient();
        patient.setPatientId(pid);
        patient.setEhrId(eid);
        patient.setPdfData(docxData); // Keep using pdfData attribute
        return patientRepository.save(patient);
    }

    // Fetch existing DOCX from the database
    public byte[] fetchDocx(String pid) {
        Patient patient = patientRepository.findById(pid)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with ID: " + pid));
        return patient.getPdfData(); // Keep using pdfData attribute
    }

    // Update existing DOCX by appending content from another DOCX file
    public void updateDocx(String pid, MultipartFile docxFile) throws IOException {
        // Fetch existing DOCX
        byte[] existingDocxData = fetchDocx(pid);

        // Load existing and new DOCX files
        XWPFDocument existingDocument = new XWPFDocument(new ByteArrayInputStream(existingDocxData));
        XWPFDocument newDocument = new XWPFDocument(docxFile.getInputStream());

        // Append paragraphs from the new DOCX to the existing DOCX
        for (XWPFParagraph paragraph : newDocument.getParagraphs()) {
            existingDocument.createParagraph().createRun().setText(paragraph.getText());
        }

        // Save the updated DOCX
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        existingDocument.write(outputStream);
        existingDocument.close();
        newDocument.close();

        // Update the patient's DOCX file in the database
        Patient patient = patientRepository.findById(pid)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with ID: " + pid));
        patient.setPdfData(outputStream.toByteArray()); // Keep using pdfData attribute
        patientRepository.save(patient);
    }
}
