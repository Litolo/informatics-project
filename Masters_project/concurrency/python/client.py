import socket

def main():
    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.connect(("localhost", 5555))

    message = client.recv(1024).decode()
    print(message)
    message = client.recv(1024).decode()
    print(message)

    while True:
        move = input("Your Move: ").strip().lower()
        client.send(move.encode())
        
        if move == "exit":
            print("You disconnected.")
            break
        
        response = client.recv(1024).decode()
        print(response)

    client.close()

if __name__ == "__main__":
    main()
