package com.blockchain.EHR.controller;

import com.blockchain.EHR.Repository.PatientRepository;
import com.blockchain.EHR.model.Patient;
import com.blockchain.EHR.services.PdfService;
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

import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@Validated
@RequestMapping("/fabric")
public class PatientController {

    @Autowired
    private PdfService pdfService;
    @Autowired
    PatientRepository patientRepository;

    @PostMapping("/patient/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("pid")String pid,
                                       @RequestParam("eid")String eid,
                                       @RequestParam("file")MultipartFile pdf){
        System.out.println("Received upload request for PID: " + pid + ", EID: " + eid);
        try {
            Patient patient = pdfService.upload(pid, eid, pdf);
            System.out.println("PDF uploaded successfully: " + patient.getPatientId());
            return new ResponseEntity<>(patient, HttpStatus.CREATED);
        } catch (Exception e) {
            System.err.println("Error during PDF upload: " + e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
//    @GetMapping("/patient/{pid}/pdf")
//    public ResponseEntity<byte[]>getPdf(@PathVariable String pid){
//        Optional<Patient>patientOptional=patientRepository.findById(pid);
//        byte[] pdfFile=patientOptional.get().getPdfData();
//        return ResponseEntity.ok().body(pdfFile);
//    }
    @GetMapping("/patient/{pid}/pdf")
    public ResponseEntity<byte[]> getPdf(@PathVariable String pid) {
        Optional<Patient> patientOptional = patientRepository.findById(pid);

        if (patientOptional.isPresent()) {
            byte[] pdfFile = patientOptional.get().getPdfData();

            // Set the appropriate content type for PDF
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=patient_" + pid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfFile);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}

