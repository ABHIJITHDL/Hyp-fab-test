package com.blockchain.EHR.Repository;

import com.blockchain.EHR.model.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PatientRepository extends MongoRepository<Patient,String> {

}
