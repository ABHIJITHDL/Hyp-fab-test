package com.blockchain.EHR.services;

import com.blockchain.EHR.model.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public List<Transaction> getHistory(String pid,String did,String mspId) throws JsonProcessingException {
        String[] args = {pid,did};
        System.out.println("Submitting transaction");
        String response = fabricService.submitTransaction("mychannel","ehr","getEHRRecord",args,pid,mspId);
        System.out.println("Response"+response);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response);

        List<Transaction> transactions = new ArrayList<>();
        for (JsonNode transactionNode : rootNode.path("transactions")){
            Transaction transaction = new Transaction();
            transaction.setType(transactionNode.path("type").asText());
            transaction.setTimestamp(transactionNode.path("timestamp").asText());
            transaction.setHash(transactionNode.path("hash").asText());
            transactions.add(transaction);
        }
        return transactions;
    }
}
