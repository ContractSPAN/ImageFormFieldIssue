package com.contractspan;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;


public class App 
{
    public static void main( String[] args ) throws Exception {
        String inputFilePath = "input.pdf";
        String pdfWithSignatureFormFields = "output/pdfWithSignatureFormFields.pdf";
        String pdfWithSignatureAndImageFormFields = "output/pdfWithSignatureAndImageFormFields.pdf";
        String signedPdfWithImageFormField = "output/signedPdfWithImageFormField.pdf";
        String signedAndImageFilledPdf = "output/signedAndImageFilledPdf.pdf";

        // Create signatory details
        List<SignatureDetails> signatureDetailsList = getSignatureDetailsList(inputFilePath);

        // // Add signature field
        PdfService.addSignatureFields(inputFilePath, pdfWithSignatureFormFields, signatureDetailsList);

        // Add initial field
        PdfService.addInitialFields(pdfWithSignatureFormFields, pdfWithSignatureAndImageFormFields);
        
        // // Add first signature
        PdfService.sign(pdfWithSignatureAndImageFormFields, signedPdfWithImageFormField, signatureDetailsList.get(0));
        
        // // Fill initial field
        PdfService.fillInitialField(signedPdfWithImageFormField, signedAndImageFilledPdf);
    }

    public static List<SignatureDetails> getSignatureDetailsList(String inputFilePath) throws IOException {
        PDDocument document = PDDocument.load(new File(inputFilePath));
        List<SignatureDetails> signatureDetailsList = new ArrayList<SignatureDetails>();

        // Create signature details
        signatureDetailsList.add(
            SignatureDetails.builder()
                .signerName("Naina Malviya")
                .contactInfo("naina@contractspan.com")
                .reason("Agree")
                .location("Hyderabad")
                .signatureImageFilePath("NainaSign.png")
                .signatureID("91823675234")
                .page(0)
                .build()
        );

        for (SignatureDetails signatureDetails : signatureDetailsList) {
            signatureDetails.createVisibleSignature(document);
        }

        // Close document
        document.close();

        return signatureDetailsList;
    }
}