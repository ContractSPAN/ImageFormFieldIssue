# Image Form Field Issue

## Introduction
I wanted to add an image form field to the pdf in which I could later attach the image. After reading various source, I found that there is no image form field in PDF. However, we can use the PDPushButton which would behave as the image form. 

Other than this PDPushButton form field there could be other form fields in the pdf such as signature form field. If the signature form field is signed before the image is attached to the PDPushButton form field then the attached signature become invalid.

## File Description
App.Java                - Contains the main function.
PdfService.java         - Contains the code to attach the form fields as well as the code to fill the form fields
SignatureService.java   - Provide functions to load certificates and keys
SignatureDetails.java   - Helper class to encapsulate all the data related to the signer

The output folder contains the generated PDFs at each stage. The final generated PDF which is signedAndImageFilledPdf.pdf have invalid signature attached to it.

## References
This repository reproduces this issue. I have implemented the PDPushButton form field by following the methods described in the links below;
- https://stackoverflow.com/questions/46799087/how-to-insert-image-programmatically-in-to-acroform-field-using-java-pdfbox
- https://github.com/mkl-public/testarea-pdfbox2/issues/1
- https://stackoverflow.com/questions/39958843/how-to-import-an-icon-to-a-button-field-in-a-pdf-using-pdfbox
- https://stackoverflow.com/questions/39958843/how-to-import-an-icon-to-a-button-field-in-a-pdf-using-pdfbox

