package com.blockchain.EHR.repository;

import com.blockchain.EHR.model.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface    PatientRepository extends MongoRepository<Patient,String> {

}
