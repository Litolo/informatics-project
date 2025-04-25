use std::fs::File;
use std::io::Read;
use openssl::rsa::{Rsa, Padding};
use mail_parser::MessageParser;
use openssl::base64;


pub fn decrypt(bytes_email_path:&str, priv_key_path:&str, password:&str) -> String {
    // read private key
    let mut file = File::open(priv_key_path).unwrap(); 
    let mut key_pem = Vec::new();
    let _ = file.read_to_end(&mut key_pem);

    let rsa = Rsa::private_key_from_pem_passphrase(&key_pem, password.as_bytes()).unwrap();
    // read email content
    let mut file = File::open(bytes_email_path).unwrap(); 
    let mut email_bytes = Vec::new();
    let _ = file.read_to_end(&mut email_bytes);
    let parsed_email = MessageParser::default().parse(&email_bytes).unwrap();

    // get email body, base64 decode it
    let encrypted_data = base64::decode_block(&parsed_email.body_text(0).unwrap()).unwrap();
    
    // decrypt email content
    let mut decrypted = vec![0; rsa.size() as usize]; // Allocate buffer for decrypted data
    let len = rsa.private_decrypt(&encrypted_data, &mut decrypted, Padding::PKCS1).unwrap();
    decrypted.truncate(len);

    return String::from_utf8(decrypted).unwrap();
}