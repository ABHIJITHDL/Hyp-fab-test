package com.blockchain.EHR.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
public class FabricUser implements User {
    private String name;
    private String mspId;
    private Enrollment enrollment;

    @Override
    public Set<String> getRoles() {
        return Set.of();
    }

    @Override
    public String getAccount() {
        return "";
    }

    @Override
    public String getAffiliation() {
        return "";
    }
}