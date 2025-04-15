package com.blockchain.EHR.controller;


import com.blockchain.EHR.jwt.JwtUtils;
import com.blockchain.EHR.model.EhrDocument;
import com.blockchain.EHR.model.Patient;
import com.blockchain.EHR.model.Pending;
import com.blockchain.EHR.model.Transaction;
import com.blockchain.EHR.repository.PatientRepository;
import com.blockchain.EHR.repository.PendingRepository;
import com.blockchain.EHR.services.EhrService;
import com.blockchain.EHR.services.PatientService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@Validated
@RequestMapping("/fabric/patient")
public class PatientController {
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PdfService pdfService;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private PatientService patientService;
    @Autowired
    private PendingRepository pendingRepository;
    @Autowired
    private EhrService ehrService;


    @GetMapping("/accepted")
    public ResponseEntity<List<Pending>> getAcceptedDoctors(HttpServletRequest request) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        String pid = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);

        try {
            List<Pending> doctorIds = patientService.getDoctors(pid, mspId);
            return new ResponseEntity<>(doctorIds, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/view-ehr")
    public ResponseEntity<EhrDocument> viewEhr(HttpServletRequest request) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        String patientId = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);
        if (!"Org2MSP".equals(mspId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        EhrDocument ehrDocument= ehrService.getEhrDocumentForPatient(patientId,mspId);
        if(ehrDocument!=null){
            System.out.println("Returning document");
            return new ResponseEntity<>(ehrDocument,HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/revoked")
    public ResponseEntity<List<String>> getRevokedDoctors(HttpServletRequest request) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        String pid = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);

        List<String> doctorIds = patientService.getRevokedDoctors(pid, mspId);
        return new ResponseEntity<>(doctorIds, HttpStatus.OK);
    }


    @GetMapping("/request")
    public List<Pending> getPendingRequests(HttpServletRequest request){
        System.out.println("pending request");
        String jwt = jwtUtils.getJwtFromHeader(request);
        String pid = jwtUtils.getUserNameFromJwtToken(jwt);
        return patientService.getPendingRequest(pid);
    }

    @PostMapping("/request/{did}")
    public void updatePendingRequest(HttpServletRequest request,
                                     @PathVariable String did,
                                     @RequestParam String status){
        System.out.println("Update Pending");
        String jwt = jwtUtils.getJwtFromHeader(request);
        String pid = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);
        try {
            patientService.updateStatus(pid,did,status,mspId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/history/{did}")
    public ResponseEntity<List<Transaction>> getDoctorHistory(HttpServletRequest request, @PathVariable String did) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        String pid = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);

        try {
            System.out.println("calling GetHistory");
            List<Transaction> transactions = patientService.getHistory(pid, did , mspId);
            return new ResponseEntity<>(transactions, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}

