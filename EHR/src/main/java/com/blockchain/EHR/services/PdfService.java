package com.blockchain.EHR.services;


import com.blockchain.EHR.model.Patient;
import com.blockchain.EHR.repository.PatientRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

@Service
public class PdfService {

    @Autowired
    private PatientRepository patientRepository;

    // Upload PDF (store patient and file)
    public Patient upload(String pid, MultipartFile pdf) throws IOException {
        Patient patient = new Patient();
        patient.setPatientId(pid);
        patient.setEhrId(pid);
        patient.setPdfData(pdf.getBytes());
        return patientRepository.save(patient);
    }

    // Fetch existing PDF by Patient ID
    public byte[] fetchPdf(String pid) {
        Patient patient = patientRepository.findById(pid)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with ID: " + pid));
        return patient.getPdfData();
    }

    public String getHash(byte[] pdf){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pdf);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {

        }
        return " ";
    }

   ; // Add a new page with paragraphs and return the updated PDF as a byte array
//    public byte[] addPagesToPdf(byte[] existingPdfData, String newText) throws IOException {
//        PDDocument document = PDDocument.load(existingPdfData);
//
//        // Split the input text into lines (newlines separate paragraphs)
//        String[] lines = newText.split("\n");
//
//        // Create a new page and add it to the document
//        PDPage newPage = new PDPage();
//        document.addPage(newPage);
//
//        // Create a content stream for the new page
//        PDPageContentStream contentStream = new PDPageContentStream(document, newPage);
//        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);  // Use your desired font
//        contentStream.beginText();
//
//        // Set the starting position for the text (adjust as needed)
//        float margin = 100;
//        float yStart = 700;
//        float leading = 14f; // Line spacing (leading)
//
//        // Set the starting position on the page
//        contentStream.newLineAtOffset(margin, yStart);
//
//        // Process each line of text (paragraphs)
//        for (String line : lines) {
//            // Split the line into words for word wrapping
//            String[] words = line.split(" ");
//            StringBuilder currentLine = new StringBuilder();
//
//            for (String word : words) {
//                // Check if the current line exceeds the maximum allowed length (e.g., 80 characters per line)
//                if ((currentLine.length() + word.length() + 1) > 80) {  // Adjust this as needed
//                    // Write the current line to the PDF and move to the next line
//                    contentStream.showText(currentLine.toString());
//                    contentStream.newLineAtOffset(0, -leading);  // Move to the next line
//                    currentLine = new StringBuilder(); // Reset the current line
//                }
//                currentLine.append(word).append(" ");  // Add the word to the current line
//            }
//
//            // Write the remaining content in the current line
//            if (currentLine.length() > 0) {
//                contentStream.showText(currentLine.toString());
//                contentStream.newLineAtOffset(0, -leading);  // Move to the next line
//            }
//        }
//
//        contentStream.endText();
//        contentStream.close();
//
//        // Save the updated document into a byte array
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        document.save(byteArrayOutputStream);
//        document.close();
//
//        return byteArrayOutputStream.toByteArray();  // Return the updated PDF as a byte array
//    }

//    public byte[] addPagesToPdf(byte[] existingPdfData, String newText) throws IOException {
//        PDDocument document = PDDocument.load(existingPdfData);
//
//        // Split the input text into paragraphs
//        String[] paragraphs = newText.split("\n\n"); // Split on double newlines for paragraphs
//
//        // Create a new page and add it to the document
//        PDPage newPage = new PDPage();
//        document.addPage(newPage);
//
//        // Create a content stream for the new page
//        PDPageContentStream contentStream = new PDPageContentStream(document, newPage);
//        contentStream.setFont(PDType1Font.HELVETICA, 12);
//
//        // Set the page dimensions and margins
//        float margin = 50;
//        float width = newPage.getMediaBox().getWidth() - 2 * margin;
//        float startY = newPage.getMediaBox().getHeight() - margin;
//        float currentY = startY;
//        float leading = 14f; // Line spacing
//        float paragraphSpacing = 20f; // Space between paragraphs
//
//        for (String paragraph : paragraphs) {
//            // Skip empty paragraphs
//            if (paragraph.trim().isEmpty()) {
//                continue;
//            }
//
//            // Split paragraph into words
//            String[] words = paragraph.trim().split("\\s+");
//            StringBuilder line = new StringBuilder();
//
//            contentStream.beginText();
//            contentStream.newLineAtOffset(margin, currentY);
//
//            for (String word : words) {
//                // Calculate width of current line + new word
//                float lineWidth = (line.length() + word.length() + 1) * 6f; // Approximate width calculation
//
//                if (lineWidth > width) {
//                    // Write current line and move to next line
//                    contentStream.showText(line.toString());
//                    contentStream.newLineAtOffset(0, -leading);
//                    currentY -= leading;
//                    line = new StringBuilder(word + " ");
//                } else {
//                    line.append(word).append(" ");
//                }
//            }
//
//            // Write the last line of the paragraph
//            if (line.length() > 0) {
//                contentStream.showText(line.toString().trim());
//            }
//
//            contentStream.endText();
//
//            // Add space between paragraphs
//            currentY -= paragraphSpacing;
//
//            // Check if we need a new page
//            if (currentY < margin) {
//                contentStream.close();
//                newPage = new PDPage();
//                document.addPage(newPage);
//                contentStream = new PDPageContentStream(document, newPage);
//                contentStream.setFont(PDType1Font.HELVETICA, 12);
//                currentY = startY;
//            }
//        }
//
//        contentStream.close();
//
//        // Save the updated document into a byte array
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        document.save(byteArrayOutputStream);
//        document.close();
//
//        return byteArrayOutputStream.toByteArray();
//    }
public byte[] addPagesToPdf(byte[] existingPdfData, String newText) throws IOException {
    PDDocument document = PDDocument.load(existingPdfData);

    // Split the text into lines, preserving original line breaks
    String[] lines = newText.split("\n");

    // Create a new page and add it to the document
    PDPage newPage = new PDPage();
    document.addPage(newPage);

    // Create a content stream for the new page
    PDPageContentStream contentStream = new PDPageContentStream(document, newPage);
    contentStream.setFont(PDType1Font.HELVETICA, 12);

    // Set the page dimensions and margins
    float margin = 50;
    float startY = newPage.getMediaBox().getHeight() - margin;
    float currentY = startY;
    float leading = 14f; // Line spacing

    contentStream.beginText();
    contentStream.newLineAtOffset(margin, currentY);

    // Process each line exactly as it is
    for (String line : lines) {
        // Check if we need a new page
        if (currentY < margin + leading) {
            contentStream.endText();
            contentStream.close();

            // Create new page
            newPage = new PDPage();
            document.addPage(newPage);
            contentStream = new PDPageContentStream(document, newPage);
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            currentY = startY;

            contentStream.beginText();
            contentStream.newLineAtOffset(margin, currentY);
        }

        // Write the line exactly as it is
        contentStream.showText(line.trim());
        contentStream.newLineAtOffset(0, -leading);
        currentY -= leading;
    }

    contentStream.endText();
    contentStream.close();

    // Save the updated document into a byte array
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    document.save(byteArrayOutputStream);
    document.close();

    return byteArrayOutputStream.toByteArray();
}

    // Update the PDF in the database with the new content
    public Patient updatePdf(String pid, String newText) throws IOException {
        // Fetch the existing PDF data
        byte[] existingPdfData = fetchPdf(pid);

        // Add the new page with the updated content (paragraphs)
        byte[] updatedPdfData = addPagesToPdf(existingPdfData, newText);

        // Find the patient and update the PDF data
        Patient patient = patientRepository.findById(pid)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with ID: " + pid));
        patient.setPdfData(updatedPdfData);

        // Save the updated patient data with the new PDF
        return patientRepository.save(patient);
    }
}
