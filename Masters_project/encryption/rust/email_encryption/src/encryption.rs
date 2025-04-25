use lettre::{
    message::header::ContentType,
    Message, SmtpTransport, Transport,
};
use url::Url;
use std::fs::{File};
use std::io::{Read, Write, BufReader};
use openssl::x509::X509;
use openssl::rsa::Padding;
use openssl::base64;

use serde::{Deserialize, Serialize};
use serde_json::from_reader;

// create the structs that define our settings.json file
#[derive(Debug, Serialize, Deserialize)]
struct SMTPServer {
    smtp: ServerDetails,
}

#[derive(Debug, Serialize, Deserialize)]
struct ServerDetails {
    host: String,
    ports: Ports,
}

#[derive(Debug, Serialize, Deserialize)]
struct Ports {
    web: String,
    smtp: String,
    imap: String,
}

pub fn encrypt(cert_path:&str, plaintext_path:&str, from:&str, to:&str, password:&str, subject:&str){
    // read the configuration settings from settings.json
    let file = File::open("../../settings.json").unwrap();
    let reader = BufReader::new(file);

    // Parse JSON into struct
    let config: SMTPServer = from_reader(reader).expect("Failed to parse JSON");

    // read certificate
    let mut file = File::open(cert_path).unwrap(); // file is automatically closed when out of scope :)
    let mut cert_pem = Vec::new();
    let _ = file.read_to_end(&mut cert_pem);

    // extract RSA public key
    let cert = X509::from_pem(&cert_pem).unwrap();
    let public_key = cert.public_key().unwrap();
    let rsa = public_key.rsa().unwrap();

    let mut encrypted = vec![0; rsa.size() as usize]; // Allocate buffer for encrypted data

    let mut f = File::open(plaintext_path).unwrap();
    let mut plaintext = String::new();
    let _ = f.read_to_string(&mut plaintext); // read plaintext file as string
    let len = rsa.public_encrypt(plaintext.as_bytes(), &mut encrypted, Padding::PKCS1).unwrap();
    encrypted.truncate(len);

    // Build a simple multipart message
    let email = Message::builder()
    .from(from.parse().unwrap())
    .to(to.parse().unwrap())
    .subject(subject)
    .header(ContentType::TEXT_PLAIN)
    .body(base64::encode_block(&encrypted))
    .unwrap();

    // don't touch this line. Add the parts of the URL below.
    let mut url = Url::parse("foo://bar").unwrap();

    // configure the scheme (`smtp` or `smtps`) here.
    url.set_scheme("smtp").unwrap();
    // configure the username and password.
    // remove the following two lines if unauthenticated.
    url.set_username(from).unwrap();
    url.set_password(Some(password)).unwrap();
    // configure the hostname, needs to be of type &str
    url.set_host(Some((config.smtp.host).as_ref())).unwrap();
    // configure the port - only necessary if using a non-default port
    let smtp_port: u16 = (config.smtp.ports.smtp).parse().unwrap();
    url.set_port(Some(smtp_port)).unwrap();
    // configure the EHLO name
    url.set_path("ehlo-name");
    // create connection with SMTP server
    let mailer = SmtpTransport::from_url(&url.to_string())
        .unwrap()
        .build();

    // write email to file
    let mut f = File::create(from.to_owned()+"_email.eml").unwrap();
    f.write_all(&email.formatted()).unwrap();
    // Send the email
    match mailer.send(&email) {
        Ok(_) => println!("Email sent successfully!"),
        Err(e) => panic!("Could not send email: {e:?}"),
   }
}