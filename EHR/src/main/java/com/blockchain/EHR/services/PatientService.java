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
        if(response.startsWith("Transaction"))
            return new ArrayList<>();
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
            if(pending.getStatus().equals("Requested"))
                doctors.add(pending.getDid());
        }
        return doctors;
    }

    public void updateStatus(String pid, String did, String status,String mspId) throws Exception {
        Pending pending = pendingRepository.findByPidAndDid(pid,did);
        EhrDocument ehrDocument = ehrService.fetchPdf(pid);
        String hash = ehrService.getHash(ehrDocument);
        System.out.println(status);
        String s;
        if(status.equals("Accepted")){
            String[] args = {pid,did};
            String response = fabricService.submitTransaction("mychannel","ehr","getEHRRecord",args,pid,mspId);
            if(response.startsWith("Transaction")){
                System.out.println("creating EHR");
                    String[] create = {did,pid,hash, LocalDate.now().toString()};
                    s =fabricService.submitTransaction("mychannel","ehr","createEHRRecord",create,pid,mspId);
                    if(s.startsWith("Transaction"))
                        throw new RuntimeException("EHR creation failed:"+s);
            }else {
                String[] activate = {did, pid, hash, LocalDate.now().toString()};
                s = fabricService.submitTransaction("mychannel", "ehr", "activateAccess", activate, pid, mspId);
                if (s.startsWith("Transaction"))
                    throw new RuntimeException("EHR access update failed: "+s);
            }
        }else if(status.equals("Revoke")) {
            String[] activate = {did, pid,LocalDate.now().toString()};
            s = fabricService.submitTransaction("mychannel", "ehr", "revokeAccess", activate, pid, mspId);
            if (s.startsWith("Transaction"))
                throw new RuntimeException("EHR revoke update failed: "+s);
        }
        pending.setStatus(status);
        pendingRepository.save(pending);
    }
}
