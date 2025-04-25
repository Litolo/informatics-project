use std::net::TcpStream;
use std::io::{self, Write, Read};

fn main() {
    let mut stream = TcpStream::connect("127.0.0.1:5555").unwrap();
    println!("Connected to the server!");

    loop {
        println!("Enter your choice (rock, paper, scissors):");
        let mut input = String::new();
        io::stdin().read_line(&mut input).unwrap();
        let choice = input.trim().to_string();

        if choice == "rock" || choice == "paper" || choice == "scissors" {
            stream.write(choice.as_bytes()).unwrap();
            stream.flush().unwrap();

            let mut buffer = [0; 1280]; // we need a sufficiently sized buffer to store the response
            stream.read(&mut buffer).unwrap();
            let response = String::from_utf8_lossy(&buffer[..]).trim().to_string();
            println!("{}", response);
            break;
        } else {
            println!("Invalid choice. Please try again.");
        }
    }
}