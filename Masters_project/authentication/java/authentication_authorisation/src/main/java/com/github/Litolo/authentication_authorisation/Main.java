package com.github.Litolo.authentication_authorisation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.security.auth.login.LoginException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Random;
// import com.github.Litolo.authentication_authorisation;
/**
 * Hello world!
 */
public class Main {
    public static void main(String[] args) {
        List<Long> createDBTimes = new ArrayList<>();
        List<Long> createTableTimes = new ArrayList<>();
        List<Long> createAccountTimes = new ArrayList<>();
        List<Long> createPostTimes = new ArrayList<>();
        List<Long> readPostTimes = new ArrayList<>();
   
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode;
        try{
            jsonNode = objectMapper.readTree(new File("../../settings.json"));
        }
        catch(IOException e){
            System.out.println(e.getMessage()); 
            return;
        }
        JsonNode mysql_config = jsonNode.get("mysql");
        Accounts accounts = new Accounts();
        accounts.setConfig(mysql_config);
        accounts.create_database("masters_project_users");
        accounts.create_users_table("masters_project_users");
        // make alice and bob accounts
        accounts.create_account("Alice", "Alice123");
        accounts.create_account("Bob","Bob123");
        accounts.create_account("/etc/passwords","password"); // try to do path traversal injection

        Authorisation auth = new Authorisation();
        auth.setConfig(mysql_config);
        try{
            auth.createPost("My First Post", "This is a sample message to demonstrate that Alice can create posts",
            "Alice", "Alice123");
            String content = auth.readPost("My First Post","Alice","Bob","Bob123");
            System.out.println(content);
            auth.createPost("", "I am up to no good", "/etc/passwords", "password");
            auth.readPost("My First Post","Alice","Bob","An invalid password"); // try Bob's account with an invalid password
        }
        catch(LoginException e){
            // our custom login exception. Invalid username/password, we don't want to give more information than needed
            // We do not change our exception messages deeper in the code because we want them to be as informative as possible
            // to help developers when debugging. We obfuscate it here because this is what the end user will see
            System.err.println("Login failed. Invalid username or password."); 
        }
        catch(Exception e){
            System.out.println("Something went wrong...");
        }
        
        for (int i = 0; i < 100; i++) {
            // Measure makeCert for Alice
            Accounts acc = new Accounts();
            acc.setConfig(mysql_config);
            long startTime = System.nanoTime();
            acc.create_database("masters_project_users");
            long endTime = System.nanoTime();
            createDBTimes.add(endTime - startTime);

            startTime = System.nanoTime();
            acc.create_users_table("masters_project_users");
            endTime = System.nanoTime();
            createTableTimes.add(endTime - startTime);

            startTime = System.nanoTime();
            acc.create_account("Alice", "Alice123");
            endTime = System.nanoTime();
            createAccountTimes.add(endTime - startTime);

            Authorisation authoris = new Authorisation();
            authoris.setConfig(mysql_config);
            startTime = System.nanoTime();
            try{
                authoris.createPost("Post", "content", "Alice", "Alice123");
            }
            catch(Exception e){}; // just ignore if theres any issues
            
            endTime = System.nanoTime();
            createPostTimes.add(endTime - startTime);
           
            startTime = System.nanoTime();
            try{
                authoris.readPost("Post", "Alice", "Alice", "Alice123");
            }
            catch(Exception e){}; // just ignore if theres any issues
            endTime = System.nanoTime();
            readPostTimes.add(endTime - startTime);
        }
        final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(30);
        for (int i = 0; i < 30; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
     
        try (FileWriter csvWriter = new FileWriter(String.format("execution_times_auth_java_%s.csv",sb.toString()))) {
            // Write the header
            csvWriter.append("createDB,createTable,createAccount,createPost,readPost\n");

            // Write the data
            for (int i = 0; i < 100; i++) {
                csvWriter.append(String.valueOf(createDBTimes.get(i))).append(",");
                csvWriter.append(String.valueOf(createTableTimes.get(i))).append(",");
                csvWriter.append(String.valueOf(createAccountTimes.get(i))).append(",");
                csvWriter.append(String.valueOf(createPostTimes.get(i))).append(",");
                csvWriter.append(String.valueOf(readPostTimes.get(i))).append("\n");
            }
        } catch (IOException e) {
            System.out.println("Error writing to CSV file: " + e.getMessage());
        }
    }
}

