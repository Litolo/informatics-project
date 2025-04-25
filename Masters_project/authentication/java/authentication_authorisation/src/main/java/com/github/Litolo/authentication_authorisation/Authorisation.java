package com.github.Litolo.authentication_authorisation;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import javax.security.auth.login.LoginException;

import com.fasterxml.jackson.databind.JsonNode;

public class Authorisation {
    private static JsonNode config;

    public void setConfig(JsonNode _config){
        config = _config;
    }

    private String sanitiseFilename(String part) {
        // Replace risky characters with underscores
        String sanitized = part
            .replace("/", "%2F")    // Encode forward slash
            .replace("\\", "%5C");   // Encode backslash
        return sanitized;
    }

    private void validatePath(Path fullPath, Path baseDir) throws IOException {
        // Ensure the path is within the base directory
        if (!fullPath.normalize().startsWith(baseDir.normalize())) {
            throw new IOException("Path traversal attempt detected");
        }
    }

    public String readPost(String title, String target_user, String username, String password) 
            throws IOException, SQLException, NoSuchAlgorithmException, LoginException {
        Login login = new Login();
        login.setConfig(config);
        login.login("masters_project_users", username, password);

        // Sanitize inputs
        String safeTargetUser = sanitiseFilename(target_user);
        String safeTitle = sanitiseFilename(title);

        // Define base directory
        Path baseDir = Paths.get("../../posts").toAbsolutePath().normalize();
        Path fullPath = baseDir.resolve(safeTargetUser + "_" + safeTitle + ".txt");

        // Validate path
        validatePath(fullPath, baseDir);

        return new String(Files.readAllBytes(fullPath), StandardCharsets.UTF_8);
    }

    public void createPost(String title, String content, String username, String password) 
            throws SQLException, IOException, NoSuchAlgorithmException, LoginException {
        Login login = new Login();
        login.setConfig(config);
        login.login("masters_project_users", username, password);

        // Sanitize inputs
        String safeUsername = sanitiseFilename(username);
        String safeTitle = sanitiseFilename(title);

        // Define base directory
        Path baseDir = Paths.get("../../posts").toAbsolutePath().normalize();
        Path fullPath = baseDir.resolve(safeUsername + "_" + safeTitle + ".txt");

        // Validate path
        validatePath(fullPath, baseDir);

        Files.createDirectories(baseDir); // Ensure directory exists
        try (FileWriter myWriter = new FileWriter(fullPath.toFile())) {
            myWriter.write(content);
        }
    }
}