package com.github.Litolo.authentication_authorisation;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import javax.security.auth.login.LoginException;
import org.apache.commons.codec.binary.Hex;
import com.fasterxml.jackson.databind.JsonNode;

public class Login {
    private static JsonNode config;

    public void setConfig(JsonNode _config){
        config = _config;
    }
    // A decision was made to NOT handle exceptions within this function. They should be handled from the 
    // place calling this function as it allows easier control over what to show to the client (the user)
    public ResultSet login(String db_name, String username, String password) throws SQLException, NoSuchAlgorithmException, LoginException{
        Connection conn = null;
        PreparedStatement stmnt = null;
        ResultSet rs = null;

            conn =
               DriverManager.getConnection("jdbc:mysql://" + config.get("host").asText()+"/"+db_name+"?" +
                                           "user=" + config.get("user").asText() +
                                           "&password=" + config.get("password").asText());
            String prepared_query = "SELECT salt FROM users WHERE username = ? LIMIT 0, 1";
            
            // Prepare Statement
            stmnt = conn.prepareStatement(prepared_query);
            stmnt.setString(1, username);
            rs = stmnt.executeQuery();
            // if there are no rows is resultSet, there is no account for this user. We don't want to say that
            if (!rs.isBeforeFirst()){
                throw new LoginException("Login failed. Username does not exist.");
            }
            rs.next();
            // decode the result from base64 string
            byte[] salt = Base64.getDecoder().decode(rs.getString(1));
            // convert the salt and password to hex
            // this is the exact same code from Accounts.java --> consider making this a seperate static function
            MessageDigest unsalted_digest;
            MessageDigest salted_digest;
            unsalted_digest = MessageDigest.getInstance("SHA3-512");
            unsalted_digest.update(password.getBytes());
            byte[] unsalted_byte_digest = unsalted_digest.digest();

            salted_digest = MessageDigest.getInstance("SHA3-512");
            // java doesnt allow us to concat two byte arrays directly so we need to copy the arrays and add them to a new one
            byte[] combined = new byte[unsalted_byte_digest.length + salt.length];
            System.arraycopy(unsalted_byte_digest, 0, combined, 0, unsalted_byte_digest.length);
            System.arraycopy(salt, 0, combined, unsalted_byte_digest.length, salt.length);
            salted_digest.update(combined);
            
            byte[] salted_byte_digest = salted_digest.digest();

            // connect to users and verify the account credentials

            prepared_query = "SELECT * FROM users WHERE username = ? AND password = ? LIMIT 0, 1";
 
            // Prepare Statement
            stmnt = conn.prepareStatement(prepared_query);
            stmnt.setString(1, username);
            stmnt.setString(2, Hex.encodeHexString(salted_byte_digest));
            rs = stmnt.executeQuery();
            if (!rs.isBeforeFirst()){
                // we can be as informative as we want here because this login function will be called by another function. 
                // We can obfuscate the message there.
                throw new LoginException("Login failed. Username and password is pair does not exist. "+
                "ResultSet is empty. Check if the username and password are both correct.");
            }
            return rs;
    }
}