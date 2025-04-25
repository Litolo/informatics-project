package com.github.Litolo.email_encryption;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;


import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;

public class Decryption {
    public static String decrypt(String bytes_email_path, String priv_key_path, final String password) throws FileNotFoundException, MessagingException, IOException, OperatorCreationException, PKCSException,
        NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    
        MimeMessage email = null;
        java.util.Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props);
        FileInputStream fis = new FileInputStream(bytes_email_path);

        email = new MimeMessage(session, fis);

        PEMParser pemParser = new PEMParser(new FileReader(priv_key_path));

        PKCS8EncryptedPrivateKeyInfo privateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) pemParser.readObject();
        pemParser.close();
        InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password.toCharArray());

        PrivateKey privateKey = new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo.decryptPrivateKeyInfo(decryptorProvider));
        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decoded = Base64.getDecoder().decode((String) email.getContent());
        byte[] decryptedMessageBytes = decryptCipher.doFinal(decoded);
        String decryptedMessage = new String(decryptedMessageBytes, StandardCharsets.UTF_8);

        return decryptedMessage;
    }
}