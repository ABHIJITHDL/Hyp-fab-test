package com.blockchain.EHR.repository;

import com.blockchain.EHR.model.Pending;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PendingRepository extends MongoRepository<Pending,String> {
    public List<Pending> findAllByDid(String did);
    public Pending findByPidAndDid(String pid,String did);
}
