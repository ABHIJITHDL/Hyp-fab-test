package com.blockchain.EHR.controller;

import com.blockchain.EHR.Repository.PatientRepository;
import com.blockchain.EHR.Repository.PendingRepository;
import com.blockchain.EHR.jwt.JwtUtils;
import com.blockchain.EHR.model.Patient;
import com.blockchain.EHR.model.Pending;
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
//    @PostMapping("/request/{pid}")
//    ResponseEntity<String>sendRequest(@PathVariable String pid){
//        Pending pending=new Pending();
//        Optional<Patient>patientOptional=patientRepository.findById(pid);
//        if(patientOptional.isPresent()){
//            pending.setRequestId(//fill);
//                    // pending.setPid(pid));
//                    pending.setStatus("pending"); //default is pending
//            pendingRepository.save(pending);
//        }return ResponseEntity.badRequest().body("No such patient");
//    }

    @PostMapping("/request/{pid}")
    public ResponseEntity<String> sendRequest(HttpServletRequest request,
                                              @PathVariable String pid) {
        // Check if the patient exists in the database
        Optional<Patient> patientOptional = patientRepository.findById(pid);

        if (patientOptional.isPresent()) {
            // Create a new Pending object
            Pending pending = new Pending();
            String jwt = jwtUtils.getJwtFromHeader(request);
            String did = jwtUtils.getUserNameFromJwtToken(jwt);
            // Fill in the details of the Pending object
            pending.setRequestId(java.util.UUID.randomUUID().toString()); // Generate unique requestId
            pending.setPid(pid); // Set the patient ID
            pending.setDid(did);
            pending.setStatus("pending"); // Default status

            // Save the pending request to the repository
            pendingRepository.save(pending);

            // Return success response
            return ResponseEntity.ok("Request sent successfully. Request ID: " + pending.getRequestId());
        } else {
            // Return error response if the patient does not exist
            return ResponseEntity.badRequest().body("No such patient found with ID: " + pid);
        }
    }

}
