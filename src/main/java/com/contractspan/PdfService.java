package com.contractspan;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDFormContentStream;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDPushButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

import lombok.SneakyThrows;

public class PdfService {

    public static void addSignatureFields(String documentFilePath, String intermediateFilePath, List<SignatureDetails> signatureDetailsList) throws Exception {
        // Open document
        PDDocument document = Loader.loadPDF(new File(documentFilePath));
        
        // Get the acroForm
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            acroForm = new PDAcroForm(document);
        }
        document.getDocumentCatalog().setAcroForm(acroForm);

        for (SignatureDetails signatureDetails : signatureDetailsList) {
            // Create signature field
            addSignatureField(document, acroForm, signatureDetails);
        }

        // Save and close the document
        document.save(intermediateFilePath);
        document.close();
    }

    private static void addSignatureField(PDDocument document, PDAcroForm acroForm, SignatureDetails signatureDetails) throws IOException {
        // Create a new signature field
        PDSignatureField signatureField = new PDSignatureField(acroForm);

        // Set the field name
        signatureField.setPartialName("ContractSpan Signature");
        signatureField.getCOSObject().setString("SignatureID", signatureDetails.getSignatureID());

        // Get the widget annotation of the field
        PDAnnotationWidget widget = signatureField.getWidgets().get(0);

        // Set the rectangle position and size of the widget
        PDRectangle rect = new PDRectangle(50, 100, 100, 50);
        widget.setRectangle(rect);

        // Set the page of the widget
        // Get the first page
        PDPage page = document.getPage(signatureDetails.getPage());
        widget.setPage(page);

        // Create a new appearance stream for the widget
        PDStream stream = new PDStream(document);
        PDFormXObject form = new PDFormXObject(stream);

        // Set the bounding box of the form
        form.setBBox(rect);
        PDResources resources = new PDResources();
        form.setResources(resources);

        // Wrap the form in a PDAppearanceStream object
        PDAppearanceStream appearanceStream = new PDAppearanceStream(form.getCOSObject());

        // Create a content stream to write to the form object
        PDFormContentStream cs = new PDFormContentStream(appearanceStream);

        // Draw a signature visuals
        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
        graphicsState.setNonStrokingAlphaConstant(0.5f);
        cs.setGraphicsStateParameters(graphicsState);
        cs.setNonStrokingColor(1f, 0.5f, 0.1f);
        cs.setStrokingColor(1f, 0.5f, 0.1f);
        cs.setLineWidth(2);
        cs.addRect(rect.getLowerLeftX(), rect.getLowerLeftY(), rect.getWidth(), rect.getHeight());
        cs.fillAndStroke();

        // Draw some text inside the rectangle
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        cs.setFont(font, 12);
        cs.setNonStrokingColor(0f, 0f, 0f);
        PDExtendedGraphicsState graphicsState1 = new PDExtendedGraphicsState();
        graphicsState1.setNonStrokingAlphaConstant(1f);
        cs.setGraphicsStateParameters(graphicsState1);
        cs.beginText();
        cs.newLineAtOffset(rect.getLowerLeftX()+10, rect.getUpperRightY() - 20);
        cs.showText("Signature:");
        cs.endText();

        // Close the content stream
        cs.close();

        // Set the form as the normal appearance of the widget
        PDAppearanceDictionary appearance = new PDAppearanceDictionary();
        appearance.setNormalAppearance(appearanceStream);
        widget.setAppearance(appearance);

        // Add the widget annotation to the page
        page.getAnnotations().add(widget);

        // Add the field to the document acroform
        acroForm.getFields().add(signatureField);
    }

    public static List<PDSignature> findSignatures(PDDocument intermediateDocument, String signatureID) throws IOException {
        // Load the PDF document
        ArrayList<PDSignature> signatures = new ArrayList<PDSignature>();

        // Get the acroForm
        PDAcroForm acroForm = intermediateDocument.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            throw new RuntimeException("No AcroForm found in the document");
        }

        // Populate the map with signatory details
        PDField field = null;
        PDSignatureField signatureField = null;
        Iterator<PDField> fieldIterator = acroForm.getFieldIterator();
        while(fieldIterator.hasNext()) {
            field = fieldIterator.next();
            if (!field.getFieldType().equals(COSName.SIG.getName())) {
                continue;
            }

            signatureField = (PDSignatureField) field;
            if (!signatureField.getCOSObject().containsKey("SignatureID")) {
                continue;
            }

            if (!signatureField.getCOSObject().getString("SignatureID").equals(signatureID)) {
                continue;
            }

            if (signatureField.getSignature() != null)
            {
                throw new IllegalStateException("The signature field is already signed.");
            }

            PDSignature signature = new PDSignature();
            signatureField.getCOSObject().setItem(COSName.V, signature);
            signatures.add(signatureField.getSignature());
        }

        return signatures;
    }

    public static void sign(String inputFilePathString, String outputFilePathString, SignatureDetails signatureDetails) throws IOException, URISyntaxException, GeneralSecurityException, InvalidKeySpecException {
        // Load input file
        PDDocument inputDocument = Loader.loadPDF(new File(inputFilePathString));

        // Find and link the relevant signature field
        List<PDSignature> signatures = PdfService.findSignatures(inputDocument, signatureDetails.getSignatureID());
        signatureDetails.setPdSignature(signatures.get(0));

        PDSignature signature = signatureDetails.getPdSignature();
        // Fill the signature details
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        signature.setName(signatureDetails.getSignerName());
        signature.setContactInfo(signatureDetails.getContactInfo());
        signature.setLocation(signatureDetails.getLocation());
        signature.setReason(signatureDetails.getReason());
        signature.setSignDate(Calendar.getInstance());

        SignatureOptions signatureOptions = new SignatureOptions();
        signatureOptions.setVisualSignature(signatureDetails.getVisibleSignature());
        signatureOptions.setPage(signatureDetails.getPage());

        SignatureInterface signatureInterface = new SignatureService();
        inputDocument.addSignature(signature, signatureInterface, signatureOptions);

        FileOutputStream fos = new FileOutputStream(outputFilePathString);
        inputDocument.saveIncremental(fos);

        // Close the document
        inputDocument.close();
        fos.close();

    }

    @SneakyThrows
    public static void addInitialFields(String inputFilePath, String outputFilePath) {
        // Open document
        PDDocument document = Loader.loadPDF(new File(inputFilePath));
        
        // Get the acroForm
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
        }
        
        // Create a new signature field
        PDPushButton pdPushButtonField = new PDPushButton(acroForm);

        // Set the field name
        pdPushButtonField.setPartialName("ContractSpan Signature");
        pdPushButtonField.getCOSObject().setString("InitialID", "132323423180965");

        // Get the widget annotation of the field
        PDAnnotationWidget widget = pdPushButtonField.getWidgets().get(0);

        // Set the rectangle position and size of the widget
        PDRectangle rect = new PDRectangle(200, 300, 130, 60);
        widget.setRectangle(rect);

        // Set the page of the widget
        // Get the first page
        PDPage page = document.getPage(0);
        widget.setPage(page);

        // Add the widget annotation to the page
        page.getAnnotations().add(widget);

        // Add the field to the document acroform
        acroForm.getFields().add(pdPushButtonField);

        // Save and close document
        document.save(outputFilePath);
        document.close();
    }

    public static PDPushButton findInitial(PDDocument inputDocument, String initialID) {
        // Get the acroForm
        PDAcroForm acroForm = inputDocument.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            throw new RuntimeException("No AcroForm found in the document");
        }

        // Populate the map with signatory details
        PDField field = null;
        PDPushButton initialField = null;
        Iterator<PDField> fieldIterator = acroForm.getFieldIterator();
        while(fieldIterator.hasNext()) {
            field = fieldIterator.next();
            if (!(field instanceof PDPushButton)) {
                continue;
            }

            initialField = (PDPushButton) field;
            if (!initialField.getCOSObject().containsKey("InitialID")) {
                continue;
            }

            if (!initialField.getCOSObject().getString("InitialID").equals(initialID)) {
                continue;
            }
        }

        return initialField;
    }

    @SneakyThrows
    public static void fillInitialField(String inputFilePath, String outputFilePath) {
        // Load input file
        PDDocument document = Loader.loadPDF(new File(inputFilePath));

        // Find and link the relevant signature field
        PDPushButton initial = PdfService.findInitial(document, "132323423180965");

        PDImageXObject pdImageXObject = PDImageXObject.createFromFile("initial.png", document);
        float width = 10 * pdImageXObject.getWidth();
        float height = 10 * pdImageXObject.getHeight();

        PDAppearanceStream pdAppearanceStream = new PDAppearanceStream(document);
        pdAppearanceStream.setResources(new PDResources());
        try (PDPageContentStream pdPageContentStream = new PDPageContentStream(document, pdAppearanceStream)) {
            pdPageContentStream.drawImage(pdImageXObject, 0, 0, width, height);
        }
        pdAppearanceStream.setBBox(new PDRectangle(width, height));

        PDAnnotationWidget pdAnnotationWidget = initial.getWidgets().get(0);
        PDAppearanceDictionary pdAppearanceDictionary = pdAnnotationWidget.getAppearance();
        if (pdAppearanceDictionary == null) {
            pdAppearanceDictionary = new PDAppearanceDictionary();
            pdAnnotationWidget.setAppearance(pdAppearanceDictionary);
        }

        pdAppearanceDictionary.setNormalAppearance(pdAppearanceStream);
        initial.setReadOnly(true);

        // Mark need to update
        initial.getCOSObject().setNeedToBeUpdated(true);
        initial.getWidgets().get(0).getCOSObject().setNeedToBeUpdated(true);
        initial.getWidgets().get(0).getAppearance().getCOSObject().setNeedToBeUpdated(true);
        pdAppearanceStream.getCOSObject().setNeedToBeUpdated(true);
        pdAppearanceStream.getResources().getCOSObject().setNeedToBeUpdated(true);

        // Save and close the document
        FileOutputStream fos = new FileOutputStream(outputFilePath);
        document.saveIncremental(fos);
        document.close();
        fos.close();
    }
}