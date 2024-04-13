package com.contractspan;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDFTemplateBuilder;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDFTemplateCreator;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigBuilder;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class SignatureDetails {
    private String signerName;
    private String contactInfo;
    private String reason;
    private String location;
    private String signatureImageFilePath;
    private String signatureID;
    private int page;
    private PDSignature pdSignature;
    private InputStream visibleSignature;

    public void createVisibleSignature(PDDocument document) throws IOException {
        InputStream signatureImageStream = new FileInputStream(signatureImageFilePath);
        PDVisibleSignDesigner visibleSignDesigner = new PDVisibleSignDesigner(document, signatureImageStream, page+1);

        PDFTemplateBuilder builder = new PDVisibleSigBuilder();
        PDFTemplateCreator creator = new PDFTemplateCreator(builder);
        setVisibleSignature(creator.buildPDF(visibleSignDesigner));
    }
}
