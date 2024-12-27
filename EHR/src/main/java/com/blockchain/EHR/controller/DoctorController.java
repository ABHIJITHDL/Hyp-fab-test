package com.blockchain.EHR.controller;


import com.blockchain.EHR.jwt.JwtUtils;
import com.blockchain.EHR.model.EhrDocument;
import com.blockchain.EHR.model.Patient;
import com.blockchain.EHR.model.PatientStatus;
import com.blockchain.EHR.model.Pending;
import com.blockchain.EHR.repository.PatientRepository;
import com.blockchain.EHR.services.DoctorService;
import com.blockchain.EHR.services.EhrService;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    @Autowired
    private com.blockchain.EHR.repository.PendingRepository pendingRepository;
    @Autowired
    private EhrService ehrService;


    @PostMapping("add-request")
    public ResponseEntity<?> addRequest(HttpServletRequest request,@RequestParam("pid")String pid){
        System.out.println("Add request");
        String jwt = jwtUtils.getJwtFromHeader(request);
        String did = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);
        doctorService.addRequest(did,pid);
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

//    // View EHR document (only if pending request status is 'Accepted')
//    @GetMapping("/view-ehr")
//    public ResponseEntity<EhrDocument> viewEhr(HttpServletRequest request, @RequestParam String patientId) {
//        String jwt = jwtUtils.getJwtFromHeader(request);
//        String did = jwtUtils.getUserNameFromJwtToken(jwt); // Get doctor ID from JWT
//
//        // Check if the request status is 'Accepted'
//        Pending pendingRequest = pendingRepository.findByPidAndDid(patientId, did);
//        if (pendingRequest == null || !"Accepted".equalsIgnoreCase(pendingRequest.getStatus())) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // Access denied
//        }
//
//        // Fetch the patient's EHR document
//        Optional<Patient> patientOptional = patientRepository.findById(patientId);
//        if (patientOptional.isPresent()) {
//            EhrDocument ehrDocument = patientOptional.get().getEhrDocument();
//            return ResponseEntity.ok(ehrDocument);
//        } else {
//            return ResponseEntity.notFound().build(); // Patient not found
//        }
//    }
//
//
//    // Update EHR document (only by approved doctors)
//    @PostMapping("/update-ehr")
//    public ResponseEntity<String> updateEhr(HttpServletRequest request, @RequestParam String patientId,
//                                            @RequestBody EhrDocument updatedEhrDocument) {
//        String jwt = jwtUtils.getJwtFromHeader(request);
//        String did = jwtUtils.getUserNameFromJwtToken(jwt); // Get doctor ID from JWT
//x
//        // Check if the request status is 'Accepted'
//        Pending pendingRequest = pendingRepository.findByPidAndDid(patientId, did);
//        if (pendingRequest == null || !"Accepted".equalsIgnoreCase(pendingRequest.getStatus())) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. Doctor not approved to update.");
//        }
//
//        // Fetch the patient's record and update the EHR document
//        Optional<Patient> patientOptional = patientRepository.findById(patientId);
//        if (patientOptional.isPresent()) {
//            Patient patient = patientOptional.get();
//            patient.setEhrDocument(updatedEhrDocument); // Update the EHR document
//            patientRepository.save(patient); // Save the updated patient record
//            return ResponseEntity.ok("EHR document updated successfully!");
//        } else {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Patient not found.");
//        }
//    }
    // View EHR document (only if pending request status is 'Accepted')
    @GetMapping("/view-ehr")
    public ResponseEntity<EhrDocument> viewEhr(HttpServletRequest request, @RequestParam String patientId) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        String did = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);
        // Fetch the EHR document
        EhrDocument ehrDocument= ehrService.getEhrDocument(patientId,did,mspId);
        if(ehrDocument!=null){
            return new ResponseEntity<>(ehrDocument,HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // Update EHR document (only by approved doctors)
    @PostMapping("/update-ehr")
    public ResponseEntity<String> updateEhr(HttpServletRequest request, @RequestParam String patientId,
                                            @RequestBody EhrDocument updatedEhrDocument) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        String did = jwtUtils.getUserNameFromJwtToken(jwt); // Get doctor ID from JWT

        // Check access approval
        if (!ehrService.isAccessApproved(patientId, did)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. Doctor not approved to update.");
        }

        // Update the EHR document
        boolean isUpdated = ehrService.updateEhrDocument(patientId, updatedEhrDocument);
        if (isUpdated) {
            return ResponseEntity.ok("EHR document updated successfully!");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Patient not found.");
        }
    }

}

