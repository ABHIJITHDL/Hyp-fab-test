package com.blockchain.EHR.repository;

import com.blockchain.EHR.model.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<UserEntity, Long> {
    UserEntity findByUsername(String username);
}