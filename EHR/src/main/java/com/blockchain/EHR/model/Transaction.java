package com.blockchain.EHR.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Transaction {
    private String type;
    private String timestamp;
    private String hash;
}
