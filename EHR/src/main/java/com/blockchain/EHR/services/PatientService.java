package com.blockchain.EHR.services;

import com.blockchain.EHR.model.EhrDocument;
import com.blockchain.EHR.model.Pending;
import com.blockchain.EHR.model.Transaction;
import com.blockchain.EHR.repository.PendingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PatientService {
    @Autowired
    private FabricService fabricService;
    @Autowired
    private PendingRepository pendingRepository;

    @Autowired
    private EhrService ehrService;

    public List<String> getDoctors(String pid,String mspId) throws JsonProcessingException {
        String[] args = {pid};
        String response = fabricService.submitTransaction("mychannel","ehr","getAllEHRRecordByPatient",args,pid,mspId);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response);
        //To-Do : Create Object to receive Transaction result and manage error
        List<String> doctorIds = new ArrayList<>();
        for(JsonNode node: rootNode){
            String doctorId = node.path("doctorId").asText();
            doctorIds.add(doctorId);
        }
        return doctorIds;
    }

    public List<Transaction> getHistory(String pid,String did,String mspId) throws JsonProcessingException {
        String[] args = {pid,did};
        String response = fabricService.submitTransaction("mychannel","ehr","getEHRRecord",args,pid,mspId);
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

    public List<String> getPendingRequest(String pid) {
        List<Pending> pendingsList= pendingRepository.findAllByPid(pid);
        List<String> doctors = new ArrayList<>();
        for (Pending pending: pendingsList){
            doctors.add(pending.getDid());
        }
        return doctors;
    }

    public void updateStatus(String pid, String did, String status,String mspId) {
        Pending pending = pendingRepository.findByPidAndDid(pid,did);
        System.out.println(status);
        if(status.equals("Accept")){
            System.out.println("Accepted");
            pendingRepository.delete(pending);
            String[] args = {pid,did};
            String response = fabricService.submitTransaction("mychannel","ehr","getEHRRecord",args,pid,mspId);
            EhrDocument ehrDocument = ehrService.fetchPdf(pid);
            String hash = ehrService.getHash(ehrDocument);
            if(response.equals("Transaction failed")){
                    String[] create = {did,pid,hash, LocalDate.now().toString()};
                    fabricService.submitTransaction("mychannel","ehr","createEHRRecord",create,pid,mspId);
            }
            else{
                String[] update = {did,pid,LocalDate.now().toString()};
                fabricService.submitTransaction("mychannel","ehr","activateAccess",update,pid,mspId);
            }
        }
        pending.setStatus(status);
        pendingRepository.save(pending);
    }
}
