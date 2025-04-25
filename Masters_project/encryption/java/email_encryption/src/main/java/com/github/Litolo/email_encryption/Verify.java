package com.github.Litolo.email_encryption;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.cert.PKIXCertPathBuilderResult;
import java.util.List;
import java.util.Date;

public class Verify {

    public static boolean verifyChain(X509Certificate cert, List<X509Certificate> intermediaries, List<X509Certificate> trusted_certs) throws KeyStoreException, IOException,
        NoSuchAlgorithmException, CertificateException, InvalidAlgorithmParameterException, CertPathBuilderException
    {
        KeyStore anchors = KeyStore.getInstance(KeyStore.getDefaultType());
        anchors.load(null);
        for (X509Certificate trusted_certificate: trusted_certs){
            anchors.setCertificateEntry("root", trusted_certificate);
        }
        X509CertSelector target = new X509CertSelector();
        target.setCertificate(intermediaries.get(0));
        PKIXBuilderParameters params = new PKIXBuilderParameters(anchors, target);
        CertStoreParameters intermediates = new CollectionCertStoreParameters(intermediaries);
        params.addCertStore(CertStore.getInstance("Collection", intermediates));
        CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
        /* Validate will throw an exception on invalid chains. */
        PKIXCertPathBuilderResult r = (PKIXCertPathBuilderResult) builder.build(params);
        // only ever returns if the above chain is built
        return true;
    }

    public static boolean verifyAttributes(X509Certificate cert) {
        Date currentTime = new Date();
        
        if (cert.getNotBefore().after(currentTime)) {
            throw new IllegalArgumentException(
                String.format("Certificate is not yet valid (valid from: %s)", cert.getNotBefore())
            );
        }
        
        if (cert.getNotAfter().before(currentTime)) {
            throw new IllegalArgumentException(
                String.format("Certificate has expired (expired on: %s)", cert.getNotAfter())
            );
        }
    
        return true;
    }
}