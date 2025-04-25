package com.github.Litolo.email_encryption;
import java.security.KeyPairGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.security.Security;
import java.util.Date;
import java.security.KeyPair;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.x500.X500Name;

import java.io.FileWriter;
import java.io.StringWriter;
import java.math.BigInteger;

public class Generate {

    public static void generateCertificate() {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyGen;
        try{
            keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        }
        catch(Exception e){
            System.err.println(e.getMessage());
            return;
        }
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        Date validFrom = new Date();
        Date validTo = new Date(validFrom.getTime() + 365 * 24 * 60 * 60 * 1000L);
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
            new X500Name("CN=My Application,O=My Organisation,L=My City,C=DE"),
            BigInteger.ONE,
            validFrom,
            validTo,
            new X500Name("CN=My Application,O=My Organisation,L=My City,C=DE"),
            SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        );
        try {
            certBuilder.addExtension(
                Extension.subjectAlternativeName,
                false, // non-critical (same as Python's critical=False)
                new GeneralNames(new GeneralName[] {
                    new GeneralName(GeneralName.dNSName, "cryptography.io"),
                    new GeneralName(GeneralName.dNSName, "www.cryptography.io")
                })
            );

            certBuilder.addExtension(
                Extension.basicConstraints,
                true, // critical (same as Python's critical=True)
                new BasicConstraints(false) // ca=false
            );

            certBuilder.addExtension(
                Extension.keyUsage,
                true, // critical
                new KeyUsage(
                    KeyUsage.digitalSignature | 
                    KeyUsage.keyEncipherment | 
                    KeyUsage.cRLSign
                )
            );

            certBuilder.addExtension(
                Extension.extendedKeyUsage,
                false, // non-critical
                new ExtendedKeyUsage(new KeyPurposeId[] {
                    KeyPurposeId.id_kp_clientAuth,
                    KeyPurposeId.id_kp_serverAuth
                })
            );
            // Generate SubjectKeyIdentifier
            SubjectKeyIdentifier subjectKeyIdentifier = new JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic());
            certBuilder.addExtension(
            Extension.subjectKeyIdentifier,
            false, // non-critical
            subjectKeyIdentifier
            );
    }
    catch(Exception e){return;}
        X509Certificate certificate;
        try{
            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(keyPair.getPrivate());
            X509CertificateHolder certificateHolder = certBuilder.build(contentSigner);
            certificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);
        }
        catch(Exception e){
            System.err.println(e.getMessage());
            return;
        }
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            pw.writeObject(certificate);
            pw.flush();
            FileWriter fw = new FileWriter("certificate.pem");
            fw.write(sw.toString());
            fw.close();
        }
        catch(Exception e){
            System.err.println(e.getMessage());
            return;
        }
    }
}