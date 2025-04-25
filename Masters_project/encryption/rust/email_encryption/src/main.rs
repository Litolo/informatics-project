// use std::io::{stdout, BufWriter};
mod encryption;
mod generate_keys;
mod decryption;

use std::fs::{File, OpenOptions};
use std::io::{Read, Write};
use std::env;
use std::time::{Instant, Duration};
use dotenvy;

use rand::{rng, Rng};
use rand::distr::Alphanumeric; // need these for random naming for execution times file to avoid overwrites
fn main(){
    generate_keys::make_keys();
    // Alice encrypts and sends an email to Bob, using his public key (from his x509 certificate)
    dotenvy::from_filename("../../.env").unwrap();

    encryption::encrypt("../../keys/Bob_certificate.pem", "../../Alice_email.txt", "Alice@email.com", 
        "Bob@email.com", env::var("ALICE_EMAIL_PASSWORD").unwrap().as_ref(), "Sending sample to Bob");
    // Bob decrypts the email using his private key
    let alice_to_bob = decryption::decrypt("./Alice@email.com_email.eml", "../../keys/Bob_private_key.pem",env::var("BOB_KEY_PASSWORD").unwrap().as_ref());
    // read the original email plaintext directly
    let mut file = File::open("../../Alice_email.txt").unwrap(); 
    let mut alice_original = String::new();
    let _ = file.read_to_string(&mut alice_original);
    println!("Does Alice's original email match the one Bob decrypted?: {}", alice_to_bob == alice_original);

    // Bob encrypts and sends an email to Alice, using her public key (from her x509 certificate)
    encryption::encrypt("../../keys/Alice_certificate.pem", "../../Bob_email.txt", "Bob@email.com", 
        "Alice@email.com", env::var("BOB_EMAIL_PASSWORD").unwrap().as_ref(), "Replying to Alice");
    // Alice decrypts the email using her private key
    let bob_to_alice = decryption::decrypt("./Bob@email.com_email.eml", "../../keys/Alice_private_key.pem",env::var("ALICE_KEY_PASSWORD").unwrap().as_ref());
    let mut file = File::open("../../Bob_email.txt").unwrap(); 
    let mut bob_original = String::new();
    let _ = file.read_to_string(&mut bob_original);

    println!("Does Alice's original email match the one Bob decrypted?: {}", bob_to_alice == bob_original);

    let mut make_keys_times = Vec::new();
    let mut encrypt_times = Vec::new();
    let mut decrypt_times = Vec::new();
    
    for _ in 0..100 {
        // Measure make_keys
        let start_time = Instant::now();
        generate_keys::make_keys();
        let duration = start_time.elapsed();
        make_keys_times.push(duration.as_nanos());

        let start_time = Instant::now();
        encryption::encrypt(
            "../../keys/Bob_certificate.pem",
            "../../Alice_email.txt",
            "Alice@email.com",
            "Bob@email.com",
            env::var("ALICE_EMAIL_PASSWORD").unwrap().as_ref(),
            "Sending sample to Bob",
        );
        let duration = start_time.elapsed();
        encrypt_times.push(duration.as_nanos());
        
        // Measure decrypt for Alice to Bob
        let start_time = Instant::now();
        let alice_to_bob = decryption::decrypt(
            "./Alice@email.com_email.eml",
            "../../keys/Bob_private_key.pem",
            env::var("BOB_KEY_PASSWORD").unwrap().as_ref(),
        );
        let duration = start_time.elapsed();
        decrypt_times.push(duration.as_nanos());
    }

    let rand_string: String = rng()
        .sample_iter(&Alphanumeric)
        .take(30)
        .map(char::from)
        .collect();

    let mut file = OpenOptions::new()
        .write(true)
        .create(true)
        .truncate(true)
        .open(format!("execution_times_encryption_rust_{}.csv", rand_string))
        .unwrap();

    // Write the header
    writeln!(file, "makeCert,encryptMsg,decryptEmail").unwrap();

    // Write the data
    for i in 0..100 {
        writeln!(
            file,
            "{},{},{}",
            make_keys_times[i], encrypt_times[i], decrypt_times[i]
        )
        .unwrap();
    }
}