package com.blockchain.EHR.services;

import com.blockchain.EHR.model.PatientStatus;
import com.blockchain.EHR.model.Pending;
import com.blockchain.EHR.repository.PendingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import javax.json.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DoctorService {
    @Autowired
    private PendingRepository pendingRepository;
    @Autowired
    private FabricService fabricService;

    public List<PatientStatus> getPatientStatus(String did,String mspId) throws JsonProcessingException {
        String[] args = {did};
        String response = fabricService.submitTransaction("mychannel","ehr","getAllEHRRecordByDoctor",args,did,mspId);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response);
        List<PatientStatus> patientStatuses = new ArrayList<>();
        for(JsonNode jsonNode:rootNode){
           PatientStatus patientStatus= new PatientStatus();
           patientStatus.setPid(jsonNode.path("patientId").asText());
           patientStatus.setStatus(jsonNode.path("status").asText());
           patientStatuses.add(patientStatus);
        }
        List<Pending> pendingList = pendingRepository.findAllByDid(did);
        for(Pending pending : pendingList){
            PatientStatus patientStatus = new PatientStatus();
            patientStatus.setPid(pending.getPid());
            patientStatus.setStatus(pending.getStatus());
            patientStatuses.add(patientStatus);
        }
        return patientStatuses;

    }

    public void addRequest(String did,String pid){
        Pending pending = new Pending();
        pending.setPid(pid);
        pending.setDid(did);
        pending.setStatus("Requested");
        pendingRepository.save(pending);
    }
}
