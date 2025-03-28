package com.blockchain.EHR.controller;


import com.blockchain.EHR.jwt.JwtUtils;
import com.blockchain.EHR.model.Patient;
import com.blockchain.EHR.model.Pending;
import com.blockchain.EHR.repository.PatientRepository;
import com.blockchain.EHR.repository.PendingRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.StringTokenizer;

@RestController
@RequestMapping("/fabric")
public class PendingController {
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    PatientRepository patientRepository;
    @Autowired
    PendingRepository pendingRepository;

}
