package com.blockchain.EHR.controller;


import com.blockchain.EHR.jwt.JwtUtils;
import com.blockchain.EHR.model.EhrDocument;
import com.blockchain.EHR.model.Patient;
import com.blockchain.EHR.model.Pending;
import com.blockchain.EHR.model.Transaction;
import com.blockchain.EHR.repository.PatientRepository;
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
    com.blockchain.EHR.repository.PendingRepository pendingRepository;


    @GetMapping("/accepted")
    public ResponseEntity<List<String>> getAcceptedDoctors(HttpServletRequest request) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        String pid = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);

        try {
            List<String> doctorIds = patientService.getDoctors(pid, mspId);
            return new ResponseEntity<>(doctorIds, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/request")
    public List<String> getPendingRequests(HttpServletRequest request){
        System.out.println("pending request");
        String jwt = jwtUtils.getJwtFromHeader(request);
        String pid = jwtUtils.getUserNameFromJwtToken(jwt);
        return patientService.getPendingRequest(pid);
    }

    @PostMapping("/request/{did}")
    public void updatePendingRequest(HttpServletRequest request,
                                     @PathVariable String did,
                                     @RequestParam String status){
        String jwt = jwtUtils.getJwtFromHeader(request);
        String pid = jwtUtils.getUserNameFromJwtToken(jwt);
        String mspId = jwtUtils.getMspIdFromJwtToken(jwt);
        patientService.updateStatus(pid,did,status,mspId);
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

    @GetMapping("/view-ehr/{pid}")
    public EhrDocument getEhrOfPatient(@PathVariable String pid){
        return patientService.getEhr(pid);
    }


}

