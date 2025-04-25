package com.github.Litolo.email_encryption;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.*;
import javax.mail.internet.*;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
// reading JSON. Need it for SMTP server configuration in settings.json
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Encryption {

    public static void encrypt(String cert_path, String plaintext_path, String _from, String _to, String _password, String _subject) throws FileNotFoundException, CertificateException, 
        IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        Security.addProvider(new BouncyCastleProvider());
        // read certificate of recipient and get public key
        PEMParser pemParser = new PEMParser(new FileReader(cert_path));

        JcaX509CertificateConverter x509Converter = new JcaX509CertificateConverter();
        X509Certificate certificate = x509Converter.getCertificate((X509CertificateHolder) pemParser.readObject());
        pemParser.close();

        PublicKey recipientKey = certificate.getPublicKey();
        // get plaintext (email content)
        String plaintext = Files.readString(Path.of(plaintext_path));

        // encrypt plaintext
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, recipientKey);
        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] ciphertextBytes = encryptCipher.doFinal(plaintextBytes);

        String ciphertextEncoded = Base64.getEncoder().encodeToString(ciphertextBytes);
        // get the SMTP server settings from settings.json (file path hardcoded for simplicity)
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(new File("../../settings.json"));
        JsonNode smtp = jsonNode.get("smtp");
        
        java.util.Properties props = System.getProperties();
        props.put("mail.smtp.host", smtp.get("host").asText());
        props.put("mail.smtp.port", smtp.get("ports").get("smtp").asText()); //TLS Port
		props.put("mail.smtp.auth", "true"); //enable authentication
		props.put("mail.smtp.starttls.enable", "true"); //enable STARTTLS
		
        //create Authenticator object to pass in Session.getInstance argument
		Authenticator auth = new Authenticator() {
			//override the getPasswordAuthentication method
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(_from, _password);
			}
		};
		Session session = Session.getInstance(props, auth);
    
        // Construct the message
        Message msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(_from));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(_to));
            msg.setSubject(_subject);
            msg.setText(ciphertextEncoded);

            // Send the message.
            Transport.send(msg);
            msg.writeTo(new FileOutputStream(_from+"_email.eml"));
        } catch (Exception e) {
            // Error.
            System.out.println(e);
        }
    }
}