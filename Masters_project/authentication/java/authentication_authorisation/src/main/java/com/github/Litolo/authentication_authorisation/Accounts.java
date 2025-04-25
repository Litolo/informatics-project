package com.github.Litolo.authentication_authorisation;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.codec.binary.Hex;

public class Accounts {
    private static JsonNode config;

    public void setConfig(JsonNode _config){
        config = _config;
    }

    public void create_database(String DB_name) {
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (Exception e){
            System.out.println(e.getMessage());
            return;
        }
        Connection conn = null;
        Statement stmnt = null;
        try {
            conn =
               DriverManager.getConnection("jdbc:mysql://" + config.get("host").asText()+"/?" +
                                           "user=" + config.get("user").asText() +
                                           "&password=" + config.get("password").asText());
            String query = "CREATE DATABASE %s";
            query = String.format(query, DB_name);
 
            // Prepare Statement
            stmnt = conn.createStatement();
            stmnt.execute(query);
        } catch (SQLException ex) {
            // System.out.println("SQLException: " + ex.getMessage());
            // System.out.println("SQLState: " + ex.getSQLState());
            // System.out.println("VendorError: " + ex.getErrorCode());
        }
        finally {
            if (stmnt != null) {
                try {
                    stmnt.close();
                } catch (SQLException sqlEx) { } // ignore
        
                stmnt = null;
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException sqlEx) { } // ignore
                conn = null;
            }
        }


    }

    public void create_users_table(String DB_name) {
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (Exception e){
            System.out.println(e.getMessage());
            return;
        }
        Connection conn = null;
        Statement stmnt = null;
        try {
            conn =
               DriverManager.getConnection("jdbc:mysql://" + config.get("host").asText()+"/"+DB_name+"?" +
                                           "user=" + config.get("user").asText() +
                                           "&password=" + config.get("password").asText());
            String query = "CREATE TABLE `users` (`username` varchar(50) NOT NULL,`password` varchar(128) NOT NULL, `salt` varchar(128) NOT NULL, PRIMARY KEY (`username`)) ENGINE=InnoDB";
 
            // Prepare Statement
            stmnt = conn.createStatement();
            stmnt.execute(query);
        } catch (SQLException ex) {
            // System.out.println("SQLException: " + ex.getMessage());
            // System.out.println("SQLState: " + ex.getSQLState());
            // System.out.println("VendorError: " + ex.getErrorCode());
        }
        finally {
            if (stmnt != null) {
                try {
                    stmnt.close();
                } catch (SQLException sqlEx) { } // ignore
        
                stmnt = null;
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException sqlEx) { } // ignore
                conn = null;
            }
        }
    }

    public void create_account(String username, String password){
        // first salt and hash the password
        MessageDigest unsalted_digest;
        MessageDigest salted_digest;
        byte[] salt = new byte[16];
        // make the unsalted password hash
        try{unsalted_digest = MessageDigest.getInstance("SHA3-512");} catch(Exception  e){System.out.println(e); return;}
        unsalted_digest.update(password.getBytes());
        byte[] unsalted_byte_digest = unsalted_digest.digest();

        try{
            salted_digest = MessageDigest.getInstance("SHA3-512");
            // get random 16 byte salt
            SecureRandom.getInstanceStrong().nextBytes(salt);
            // java doesnt allow us to concat two byte arrays directly so we need to copy the arrays and add them to a new one
            byte[] combined = new byte[unsalted_byte_digest.length + salt.length];
            System.arraycopy(unsalted_byte_digest, 0, combined, 0, unsalted_byte_digest.length);
            System.arraycopy(salt, 0, combined, unsalted_byte_digest.length, salt.length);
            // finally create the hash for the salt and hashed password combined
            salted_digest.update(combined);
        } catch(Exception  e){
            // System.out.println(e); 
            return;
        }
        byte[] salted_byte_digest = salted_digest.digest();

        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (Exception e){
            System.out.println(e.getMessage());
            return;
        }
        Connection conn = null;
        PreparedStatement stmnt = null;
        // connect to salt DB and write
        try {
            // connect to users DB and write
            conn =
               DriverManager.getConnection("jdbc:mysql://" + config.get("host").asText()+"/masters_project_users?" +
                                           "user=" + config.get("user").asText() +
                                           "&password=" + config.get("password").asText());
            String prepared_query = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
 
            // Prepare Statement
            stmnt = conn.prepareStatement(prepared_query);
            stmnt.setString(1, username);
            stmnt.setString(2, Hex.encodeHexString(salted_byte_digest));
            stmnt.setString(3, Base64.getEncoder().encodeToString(salt));
            stmnt.executeUpdate();
        } catch (SQLException ex) {
            // System.out.println("SQLException: " + ex.getMessage());
            // System.out.println("SQLState: " + ex.getSQLState());
            // System.out.println("VendorError: " + ex.getErrorCode());
        }
        finally {
            if (stmnt != null) {
                try {
                    stmnt.close();
                } catch (SQLException sqlEx) { } // ignore
                stmnt = null;
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException sqlEx) { } // ignore
                conn = null;
            }
        }
    }
}
