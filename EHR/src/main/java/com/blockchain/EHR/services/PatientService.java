package com.blockchain.EHR.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PatientService {
    @Autowired
    private FabricService fabricService;

    public List<String> getDoctors(String pid,String mspId) throws JsonProcessingException {
        String[] args = {pid};
        String response = fabricService.submitTransaction("mychannel","ehr","getAllEHRRecordsForPatient",args,pid,mspId);
        System.out.println("response: "+response);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response);

        List<String> doctorIds = new ArrayList<>();
        for(JsonNode node: rootNode){
            String doctorId = node.path("doctorId").asText();
            doctorIds.add(doctorId);
        }
        return doctorIds;
    }

    public List<Map<String,String>> getHistory(String pid,String mspId){
        String[] args = {pid,""}
    }
}
