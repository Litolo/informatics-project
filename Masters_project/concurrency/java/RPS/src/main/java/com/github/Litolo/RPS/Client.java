package com.github.Litolo.RPS;

// Client.java
import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", 5555);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                System.out.println(serverMessage);
                if (serverMessage.contains("Enter move")) {
                    String move = userInput.readLine();
                    out.println(move);
                }
            }
        } catch (IOException e) {
            System.out.println("Connection closed by server.");
        }
    }
}