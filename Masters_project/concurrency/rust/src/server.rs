use std::net::{TcpListener, TcpStream};
use std::io::{Read, Write};
use std::sync::{Arc, Mutex};
use std::thread;

struct Game {
    player1: Option<(TcpStream, String)>, // Store the stream and choice for player 1
    player2: Option<(TcpStream, String)>, // Store the stream and choice for player 2
}

impl Game {
    fn new() -> Self {
        Game {
            player1: None,
            player2: None,
        }
    }

    fn add_player_choice(&mut self, stream: TcpStream, choice: String, player: u8) -> Option<String> {
        if self.player1.is_none() && player == 1{
            self.player1 = Some((stream, choice));
            None
        } else if self.player2.is_none() && player == 2{
            self.player2 = Some((stream, choice));
            Some(self.determine_winner())
        } else {
            None
        }
    }

    fn determine_winner(&mut self) -> String {
        let (mut stream1, p1) = self.player1.take().unwrap();
        let (mut stream2, p2) = self.player2.take().unwrap();

        let result = match (p1.as_str(), p2.as_str()) {
            ("rock", "scissors") | ("scissors", "paper") | ("paper", "rock") => {
                format!("Player 1 chose {} and Player 2 chose {}. Player 1 wins!", p1, p2)
            }
            ("scissors", "rock") | ("paper", "scissors") | ("rock", "paper") => {
                format!("Player 1 chose {} and Player 2 chose {}. Player 2 wins!", p1, p2)
            }
            _ => format!("Player 1 chose {} and Player 2 chose {}. It's a tie!", p1, p2),
        };
        // Send the result to both players
        let _ = stream1.write(result.as_bytes());
        let _ = stream2.write(result.as_bytes());

        result
    }
}

fn handle_client(mut stream: TcpStream, player: u8, game: Arc<Mutex<Game>>) {
    let mut buffer = [0; 512];
    stream.read(&mut buffer).unwrap();
    let choice = String::from_utf8_lossy(&buffer[..]).trim().to_string();

    println!("Received choice: {}", choice);

    let mut game = game.lock().unwrap();
    if let Some(result) = game.add_player_choice(stream, choice, player) {
        println!("Game result: {}", result);
    } else {
        println!("Waiting for the other player...");
    }
}

fn main() {
    let listener = TcpListener::bind("127.0.0.1:5555").unwrap();
    println!("Server listening on port 5555");

    let game = Arc::new(Mutex::new(Game::new()));
    let mut i = 0;
    for stream in listener.incoming() {
        i += 1;
        let stream = stream.unwrap();
        let game = Arc::clone(&game);
        thread::spawn(move || {
            handle_client(stream, i, game);
        });
    }
}