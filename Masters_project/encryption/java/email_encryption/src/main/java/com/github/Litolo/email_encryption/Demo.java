package com.github.Litolo.email_encryption;
import static com.github.Litolo.email_encryption.Encryption.encrypt;
import static com.github.Litolo.email_encryption.Generate.generateCertificate;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.cdimascio.dotenv.Dotenv;

import static com.github.Litolo.email_encryption.Decryption.decrypt;
// import static com.github.Litolo.email_encryption.Verify.verifyChain;

import java.util.Random;

public class Demo {
    public static void main(String[] args) {
        generateCertificate();
        String AliceOriginal;
        String AliceToBob;
        String BobOriginal;
        String BobToAlice;
        List<Long> makeCertTimes = new ArrayList<>();
        List<Long> encryptMsgTimes = new ArrayList<>();
        List<Long> decryptEmailTimes = new ArrayList<>();

        Dotenv dotenv = Dotenv.configure()
        .directory("../../")
        .filename(".env")
        .load();

        try{
            encrypt("../../keys/Bob_certificate.pem", "../../Alice_email.txt", "Alice@email.com", 
            "Bob@email.com", dotenv.get("ALICE_EMAIL_PASSWORD"), "Sending sample to Bob");
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
        // encrypt decrypt Alice's email to Bob
        try{AliceToBob = decrypt("./Alice@email.com_email.eml", "../../keys/Bob_private_key.pem",dotenv.get("BOB_KEY_PASSWORD"));}
        catch(Exception e){
            System.out.println(e.getMessage());
            return;
        }
        try{AliceOriginal = Files.readString(Path.of("../../Alice_email.txt"));}
        catch(IOException e){System.out.println(e); return;}

        System.out.println("Does Alice's original email match the one Bob decrypted?: " + AliceToBob.equals(AliceOriginal));

        // encrypt bob's email to Alice
        try{
            encrypt("../../keys/Alice_certificate.pem", "../../Bob_email.txt", 
            "Bob@email.com", "Alice@email.com", dotenv.get("BOB_EMAIL_PASSWORD"), "Replying to Alice");
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
        // encrypt decrypt Bob's email to Alice
        try{BobToAlice = decrypt("./Bob@email.com_email.eml", "../../keys/Alice_private_key.pem",dotenv.get("ALICE_KEY_PASSWORD"));}
        catch(Exception e){
            System.out.println(e.getMessage());
            return;
        }
        try{BobOriginal = Files.readString(Path.of("../../Bob_email.txt"));}
        catch(IOException e){System.out.println(e); return;}

        System.out.println("Does Bob's original email match the one Alice decrypted?: " + BobToAlice.equals(BobOriginal));

        for (int i = 0; i < 100; i++) {
            // Measure makeCert for Alice
            long startTime = System.nanoTime();
            try {
                // Generate Alice's certificate
                // Note: You may need to adjust this part to match your actual certificate generation logic
                // For now, we assume generateCertificate() is called here
                generateCertificate();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            long endTime = System.nanoTime();
            makeCertTimes.add(endTime - startTime);

            startTime = System.nanoTime();
            try {
                encrypt("../../keys/Bob_certificate.pem", "../../Alice_email.txt", "Alice@email.com",
                        "Bob@email.com", dotenv.get("ALICE_EMAIL_PASSWORD"), "Sending sample to Bob");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            endTime = System.nanoTime();
            encryptMsgTimes.add(endTime - startTime);

            // Measure decryptEmail for Alice to Bob
            startTime = System.nanoTime();
            try {
                AliceToBob = decrypt("./Alice@email.com_email.eml", "../../keys/Bob_private_key.pem", dotenv.get("BOB_KEY_PASSWORD"));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            endTime = System.nanoTime();
            decryptEmailTimes.add(endTime - startTime);
        }
        
        final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(30);
        for (int i = 0; i < 30; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        
        try (FileWriter csvWriter = new FileWriter(String.format("execution_times_encryption_java_%s.csv", sb.toString()))) {
            // Write the header
            csvWriter.append("makeCert,encryptMsg,decryptEmail\n");

            // Write the data
            for (int i = 0; i < 100; i++) {
                csvWriter.append(String.valueOf(makeCertTimes.get(i))).append(",");
                csvWriter.append(String.valueOf(encryptMsgTimes.get(i))).append(",");
                csvWriter.append(String.valueOf(decryptEmailTimes.get(i)));
                csvWriter.append("\n");
            }
        } catch (IOException e) {
            System.out.println("Error writing to CSV file: " + e.getMessage());
        }
    }
}
