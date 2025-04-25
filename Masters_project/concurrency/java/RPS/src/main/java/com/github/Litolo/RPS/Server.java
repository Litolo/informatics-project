package com.github.Litolo.RPS;

// Server.java
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final ConcurrentHashMap<Integer, String> moves = new ConcurrentHashMap<>();
    private static ArrayList<Integer> players = new ArrayList<Integer>();
    private static ArrayList<Thread> threads = new ArrayList<Thread>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5555);
        System.out.println("Server started on port 5555");
        ConcurrentHashMap<Integer, String> roundMoves = new ConcurrentHashMap<>();

        while (true) {
            System.out.println("Waiting for players...");
            for (int player = 1; player < 3; player++){
                try{
                    if (players.contains(player)){
                        continue;
                    }
                }
                catch(Exception e){}
                Socket player1Socket = serverSocket.accept();
                System.out.println("Player "+player+" has joined");
                players.add(player);
                Thread playerThread = new Thread(new PlayerHandler(player1Socket, player, roundMoves));
                threads.add(playerThread);
            }
            for (Thread thread: threads ){
                thread.start();
            }
            threads.clear(); // totally reset all connections
            players.clear();
            System.out.println("Round ended. Waiting for next players...");
        }
    }

    private static class PlayerHandler implements Runnable {
        private Socket socket;
        private int playerId;
        private ConcurrentHashMap<Integer, String> roundMoves;

        public PlayerHandler(Socket socket, int playerId, ConcurrentHashMap<Integer, String> roundMoves) {
            this.socket = socket;
            this.playerId = playerId;
            this.roundMoves = roundMoves;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                while (true) {
                    out.println("Welcome Player " + playerId + ". Enter move (rock, paper, scissors) or 'quit' to exit:");
                    String move = in.readLine();

                    if (move == null || move.equalsIgnoreCase("quit")) {
                        System.out.println("Player " + playerId + " disconnected.");
                        socket.close();
                        players.remove(playerId - 1);
                        return;
                    }

                    move = move.toLowerCase();
                    if (move.matches("rock|paper|scissors")) {
                        roundMoves.put(playerId, move);
                        out.println("Move accepted: " + move);
                    } else {
                        out.println("Invalid move. Please enter rock, paper, or scissors.");
                        continue;
                    }

                    while (roundMoves.size() < 2) {
                        Thread.sleep(100);
                    }

                    String result = determineWinner(roundMoves);
                    out.println(result);
                    Thread.sleep(100); // give the other thread some time to print out the result too
                    roundMoves.clear();
                    return;
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Player " + playerId + " disconnected unexpectedly.");
                roundMoves.clear();
            }
        }

        private String determineWinner(ConcurrentHashMap<Integer, String> moves) {
            String move1 = moves.get(1);
            String move2 = moves.get(2);

            if (move1 == null || move2 == null) return "One player disconnected. Round reset.";
            if (move1.equals(move2)) return "It's a tie!";
            if ((move1.equals("rock") && move2.equals("scissors")) ||
                (move1.equals("scissors") && move2.equals("paper")) ||
                (move1.equals("paper") && move2.equals("rock"))) {
                return "Player 1 wins!";
            }
            return "Player 2 wins!";
        }
    }
}