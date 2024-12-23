package com.blockchain.EHR.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EhrDocument {
    private String ehrId;               // EHR identifier (optional if already in Patient)
    private String diagnosis;           // Primary diagnosis for the patient
    private String treatment;           // Current treatment plan
    private String medications;         // List of prescribed medications
    private String doctorNotes;         // Notes added by the doctor
    private String patientHistory;      // Medical history of the patient
    private String allergies;           // Known allergies
    private String labResults;          // Summary of lab results
    private String imagingReports;      // Reports of imaging studies (e.g., X-rays, MRIs)
    private String vitalSigns;          // Recent vital signs (e.g., BP, heart rate)
    private String familyHistory;       // Family medical history
    private String lifestyleFactors;    // Lifestyle information (e.g., smoking, alcohol)
    private String immunizations;       // Immunization records
    private String carePlan;            // Future care plans and recommendations
    private String followUpInstructions; // Instructions for follow-up
}
