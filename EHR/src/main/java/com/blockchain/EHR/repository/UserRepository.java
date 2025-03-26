package com.blockchain.EHR.repository;

import com.blockchain.EHR.model.UserEntity;
import org.apache.xmlbeans.impl.xb.xsdschema.Attribute;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByUsernameAndMspId(String username, String mspId);
}