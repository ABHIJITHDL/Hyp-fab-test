package com.blockchain.EHR.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.web.bind.annotation.GetMapping;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pending")
public class Pending {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String requestId;
    private String pid;
    private String did;
    private String status;
}
