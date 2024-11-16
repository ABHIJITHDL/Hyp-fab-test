package com.blockchain.EHR.services;

import com.blockchain.EHR.Repository.PatientRepository;
import com.blockchain.EHR.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

    // Fetch existing PDF by Patient ID
    public byte[] fetchPdf(String pid) {
        Patient patient = patientRepository.findById(pid)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with ID: " + pid));
        return patient.getPdfData();
    }

    // Add a new page to an existing PDF and return the updated PDF as a byte array
    public byte[] addPageToPdf(byte[] existingPdfData, String newText) throws IOException {
        // Load the existing PDF
        PDDocument document = PDDocument.load(existingPdfData);

        // Create a new page and add it to the document
        PDPage newPage = new PDPage();
        document.addPage(newPage);

        // Add text to the new page
        PDPageContentStream contentStream = new PDPageContentStream(document, newPage);
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(100, 700); // Adjust position as needed
        contentStream.showText(newText); // Text to append
        contentStream.endText();
        contentStream.close();

        // Save the updated document into a byte array
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        document.save(byteArrayOutputStream);
        document.close();

        return byteArrayOutputStream.toByteArray();  // Return the updated PDF as a byte array
    }

    // Update the PDF in the database with the new content
    public Patient updatePdf(String pid, String newText) throws IOException {
        // Fetch the existing PDF
        byte[] existingPdfData = fetchPdf(pid);

        // Add the new page with updated content
        byte[] updatedPdfData = addPageToPdf(existingPdfData, newText);

        // Find the patient and update the PDF data
        Patient patient = patientRepository.findById(pid)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with ID: " + pid));
        patient.setPdfData(updatedPdfData);

        // Save the updated patient
        return patientRepository.save(patient);
    }
}
