package com.blockchain.EHR.repository;

import com.blockchain.EHR.model.Pending;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PendingRepository extends MongoRepository<Pending,String> {
    public List<Pending> findAllByDid(String did);
    public Pending findByPidAndDid(String pid,String did);
    public List<Pending> findAllByPid(String pid);
    public List<Pending> findAllByStatus(String status);
}
