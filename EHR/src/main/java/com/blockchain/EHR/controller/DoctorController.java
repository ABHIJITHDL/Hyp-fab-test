package com.blockchain.EHR.controller;


import com.blockchain.EHR.jwt.JwtUtils;
import com.blockchain.EHR.model.EhrDocument;
import com.blockchain.EHR.model.Patient;
import com.blockchain.EHR.model.PatientStatus;
import com.blockchain.EHR.model.Pending;
import com.blockchain.EHR.repository.PatientRepository;
import com.blockchain.EHR.repository.PendingRepository;
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

    // View EHR document (only if pending request status is 'Accepted')
    @GetMapping("/view-ehr")
    public ResponseEntity<EhrDocument> viewEhr(HttpServletRequest request, @RequestParam String patientId) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        String did = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);
        // Fetch the EHR document
        EhrDocument ehrDocument= ehrService.getEhrDocument(patientId,did,mspId);
        if(ehrDocument!=null){
            System.out.println("Returning document");
            return new ResponseEntity<>(ehrDocument,HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getAllRequests(HttpServletRequest request){
        String jwt = jwtUtils.getJwtFromHeader(request);
        String did = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);

        List<Pending> patientStatuses = pendingRepository.findAllByDid(did);
        return new ResponseEntity<>(patientStatuses,HttpStatus.OK);
    }

    // Update EHR document (only by approved doctors)
    @PostMapping("/update-ehr")
    public ResponseEntity<String> updateEhr(HttpServletRequest request, @RequestParam String patientId,
                                            @RequestBody EhrDocument updatedEhrDocument) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        String did = jwtUtils.getUserNameFromJwtToken(jwt); // Get doctor ID from JWT
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);

        // Update the EHR document
        boolean isUpdated = ehrService.updateEhr(did,patientId,mspId,updatedEhrDocument);
        if (isUpdated) {
            System.out.println("EHR document updated successfully!");
            return ResponseEntity.ok("EHR document updated successfully!");
        } else {
            System.out.println("Patient not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Patient not found.");
        }
    }

}

