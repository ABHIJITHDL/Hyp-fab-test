package com.blockchain.EHR.services;

import com.blockchain.EHR.model.EhrDocument;
import com.blockchain.EHR.model.Pending;
import com.blockchain.EHR.model.Transaction;
import com.blockchain.EHR.repository.PendingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Proposal;
import org.hyperledger.fabric.protos.peer.ProposalResponse;
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

    public List<Pending> getDoctors(String pid,String mspId) throws JsonProcessingException {
        String[] args = {pid};
        String response = fabricService.submitTransaction("mychannel","ehr","getAllEHRRecordByPatient",args,pid,mspId);
        if(response.startsWith("Transaction"))
            return new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response);
        //To-Do : Create Object to receive Transaction result and manage error
        List<Pending> doctorIds = new ArrayList<>();
        for(JsonNode node: rootNode){
                Pending doctor = new Pending();
                doctor.setRequestId(pid);
                doctor.setPid(pid);
                doctor.setDid(node.path("doctorId").asText());
                doctor.setStatus(node.path("status").asText());
                doctorIds.add(doctor);
        }
        return doctorIds;
    }

    public List<String> getRevokedDoctors(String pid, String did){
        List<String> doctors = new ArrayList<>();
        List<Pending> list = pendingRepository.findAllByStatus("Revoked");
        for(Pending item: list){
            doctors.add(item.getDid());
        }
        return doctors;
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

    public List<Pending> getPendingRequest(String pid) {
        List<Pending> pendingsList= pendingRepository.findAllByPid(pid);
        pendingsList.removeIf(pending -> !pending.getStatus().equals("Requested"));
        return pendingsList;
    }

    public void updateStatus(String pid, String did, String status,String mspId) throws Exception {
        Pending pending = pendingRepository.findByPidAndDid(pid,did);
        if (pending == null) {
            throw new RuntimeException("No pending record found for pid: " + pid + " and did: " + did);
        }
        EhrDocument ehrDocument = ehrService.fetchPdf(pid);
        String hash = ehrService.getHash(ehrDocument);
        String s;
        switch (status) {
            case "Accepted" -> {
                String[] args = {pid, did};
                String response = fabricService.submitTransaction("mychannel", "ehr", "getEHRRecord", args, pid, mspId);
                if (response.startsWith("Transaction")) {
                    System.out.println("creating EHR");
                    String[] create = {did, pid, hash, LocalDate.now().toString()};
                    s = fabricService.submitTransaction("mychannel", "ehr", "createEHRRecord", create, pid, mspId);
                    if (s.startsWith("Transaction"))
                        throw new RuntimeException("EHR creation failed:" + s);
                }else{
                    String[] activate = {did, pid, hash, LocalDate.now().toString()};
                    s = fabricService.submitTransaction("mychannel", "ehr", "activateAccess", activate, pid, mspId);
                    if (s.startsWith("Transaction"))
                        throw new RuntimeException("EHR revoke update failed: " + s);
                }
            }
            case "Revoked" -> {
                String[] activate = {did, pid, hash,LocalDate.now().toString()};
                s = fabricService.submitTransaction("mychannel", "ehr", "revokeAccess", activate, pid, mspId);
                if (s.startsWith("Transaction"))
                    throw new RuntimeException("EHR revoke update failed: " + s);

            }
            case "Activate" -> {
                String[] activate = {did, pid, hash, LocalDate.now().toString()};
                s = fabricService.submitTransaction("mychannel", "ehr", "activateAccess", activate, pid, mspId);
                if (s.startsWith("Transaction"))
                    throw new RuntimeException("EHR revoke update failed: " + s);
//            Contract contract = fabricService.getContract("mychannel", "ehr", pid, mspId);
//
//            // Build and endorse the proposal
//            try {
//                // Endorse and submit the transaction
//                Proposal proposal = contract.newProposal("activateAccess")
//                        .addArguments(activate) // Add arguments to the proposal
//                        .build();
//
//                // Endorse and submit the transaction
//                proposal.endorse().submit();
//                System.out.println("Transaction submitted successfully.");
//            } catch (org.hyperledger.fabric.client.EndorseException e) {
//                // Log detailed error information
//                System.err.println("Endorsement failed: " + e.getMessage());
//                e.getDetails().forEach(detail -> System.err.println("Detail: " + detail.getMessage()));
//            } catch (io.grpc.StatusRuntimeException e) {
//                // Log gRPC-specific error details
//                System.err.println("gRPC error: " + e.getStatus().getDescription());
//                System.err.println("gRPC status: " + e.getStatus().getCode());
//            } catch (Exception e) {
//                // Log any other exceptions
//                System.err.println("Unexpected error: " + e.getMessage());
//            }

            }
        }
        pending.setStatus(status);
        pendingRepository.save(pending);
    }
}
