package com.blockchain.EHR.repository;

import com.blockchain.EHR.model.Pending;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingRepository extends MongoRepository<Pending,String> {
}
