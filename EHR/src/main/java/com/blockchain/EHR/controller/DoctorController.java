package com.blockchain.EHR.controller;


import com.blockchain.EHR.jwt.JwtUtils;
import com.blockchain.EHR.model.Patient;
import com.blockchain.EHR.model.PatientStatus;
import com.blockchain.EHR.repository.PatientRepository;
import com.blockchain.EHR.services.DoctorService;
import com.blockchain.EHR.services.PdfService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@Validated
@RequestMapping("/fabric/doctor")
public class DoctorController {

    @Autowired
    private PdfService pdfService;
    @Autowired
    PatientRepository patientRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private DoctorService doctorService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("pid")String pid,
                                       @RequestParam("file")MultipartFile pdf){
        System.out.println("Received upload request for PID: " + pid);
        try {
            Patient patient = pdfService.upload(pid, pdf);
            System.out.println("PDF uploaded successfully: " + patient.getPatientId());
            return new ResponseEntity<>(patient, HttpStatus.CREATED);
        } catch (Exception e) {
            System.err.println("Error during PDF upload: " + e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("add-request")
    public ResponseEntity<?> addRequest(HttpServletRequest request,@RequestParam("pid")String pid){
        String jwt = jwtUtils.getJwtFromHeader(request);
        String did = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/patients")
    public ResponseEntity<?> getAllPatients(HttpServletRequest request){
        String jwt = jwtUtils.getJwtFromHeader(request);
        String did = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);

        try {
            List<PatientStatus> patientStatuses = doctorService.getPatientStatus(did,mspId);
            return new ResponseEntity<>(patientStatuses,HttpStatus.OK);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @GetMapping("/view-pdf")
    public ResponseEntity<byte[]> viewPdf(@RequestParam String patientId) {
        Optional<Patient> patientOptional = patientRepository.findById(patientId);

        if (patientOptional.isPresent()) {
            byte[] pdfFile = patientOptional.get().getPdfData();

            // Set the appropriate content type for PDF
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=patient_" + patientId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfFile);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/update-pdf")
    public ResponseEntity<String> updatePdf(@RequestParam String pid, @RequestParam String newText) {
        try {
            pdfService.updatePdf(pid, newText);
            return ResponseEntity.ok("PDF updated successfully!");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error updating PDF: " + e.getMessage());
        }
    }
}

