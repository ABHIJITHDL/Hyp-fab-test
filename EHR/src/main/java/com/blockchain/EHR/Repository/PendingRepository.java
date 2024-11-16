package com.blockchain.EHR.Repository;

import com.blockchain.EHR.model.Pending;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PendingRepository extends MongoRepository<Pending,String> {
}
