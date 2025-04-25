import socket
import threading

moves = {}
players = set()
lock = threading.Lock()

def determine_winner(move1, move2):
    if move1 == move2:
        return "Draw!"
    if (move1 == "rock" and move2 == "scissors") or \
       (move1 == "paper" and move2 == "rock") or \
       (move1 == "scissors" and move2 == "paper"):
        return "Player 1 Wins!"
    return "Player 2 Wins!"

def validate_move(move):
    return move in ["rock", "paper", "scissors"]

def handle_client(client, player):
    try:
        client.send(f"Welcome Player {player}! Type 'exit' to quit.\n".encode())
        client.send(f"Player {player}, enter rock, paper, or scissors: ".encode())
        
        while True:
            move = client.recv(1024).decode().strip().lower()

            if not move:
                print(f"Player {player} disconnected unexpectedly.")
                break

            if move == "exit":
                client.send(b"Goodbye!\n")
                print(f"Player {player} disconnected.")
                break

            if not validate_move(move):
                client.send(b"Invalid move! Try again.\n")
                continue

            with lock:
                moves[player] = move
                print(f"Player {player} chose: {move}")

            while len(moves) < 2:
                pass

            result = determine_winner(moves[1], moves[2])
            client.send(f"Result: {result}\n".encode())

            with lock:
                moves.clear()
                
    except (ConnectionResetError, BrokenPipeError):
        print(f"Player {player} disconnected unexpectedly.")
    finally:
        client.close()
        print(f"Waiting for a new player to join as Player {player}...")
        players.remove(player)

def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(("localhost", 5555))
    server.listen(2)
    print("Server is waiting for players...")

    while True:
        for player in range(1, 3):
            # this is true if a player is already connected, so we need to skip that player
            # this makes sure we cannot have two "player 1s" playing at the same time
            if player in players:
                continue
            print(f"Waiting for Player {player} to join...")
            client, addr = server.accept()
            print(f"Player {player} connected from {addr}")
            players.add(player)
            thread = threading.Thread(target=handle_client, args=(client, player))
            thread.start()

if __name__ == "__main__":
    main()