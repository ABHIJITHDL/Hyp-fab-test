package com.blockchain.EHR.model;

public class EnrollmentRequest {

    private String caName;
    private String id;
    private String password;

    // Getters and Setters
    public String getCaName() {
        return caName;
    }

    public void setCaName(String caName) {
        this.caName = caName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
