package com.contractspan;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

public class SignatureService implements SignatureInterface {
    private PrivateKey privateKey;
    private Certificate[] certificateChain;

    public SignatureService() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, CertificateException {
        String keyPath = "key.pem";
        String certPath = "cert.pem";
        this.privateKey = readPrivateKey(keyPath);
        this.certificateChain = new Certificate[]{readCertificate(certPath)};
    }

    public Certificate readCertificate(String certPath) throws CertificateException, IOException {
        try (FileReader certReader = new FileReader(certPath)) {

            PEMParser pemParser = new PEMParser(certReader);
            JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
            X509CertificateHolder certificateHolder = (X509CertificateHolder) pemParser.readObject();

            return converter.getCertificate(certificateHolder);
        }
    }

    public PrivateKey readPrivateKey(String keyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        try (FileReader keyReader = new FileReader(keyPath)) {

            PEMParser pemParser = new PEMParser(keyReader);
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(pemParser.readObject());

            return converter.getPrivateKey(privateKeyInfo);
        }
    }

    public byte[] sign(InputStream content) throws IOException
    {
        // cannot be done private (interface)
        try
        {
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            X509Certificate cert = (X509Certificate) certificateChain[0];
            ContentSigner sha1Signer = new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey);
            gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build()).build(sha1Signer, cert));
            gen.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));
            CMSProcessableByteArray msg = new CMSProcessableByteArray(content.readAllBytes());
            CMSSignedData signedData = gen.generate(msg, false);
            // if (tsaUrl != null && tsaUrl.length() > 0)
            // {
            //     ValidationTimeStamp validation = new ValidationTimeStamp(tsaUrl);
            //     signedData = validation.addSignedTimeStamp(signedData);
            // }
            
            byte[] signature = signedData.getEncoded();

            return signature;
        }
        catch (GeneralSecurityException | CMSException | OperatorCreationException e)
        {
            throw new IOException(e);
        }
    }
}
