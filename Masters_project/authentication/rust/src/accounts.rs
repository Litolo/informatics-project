use serde::{Deserialize, Serialize};
use std::fs::File;
use std::io::{BufReader};
use serde_json::from_reader;
use mysql::*;
use mysql::prelude::*;
use sha3::{Sha3_512, Digest};
use rand::rngs::OsRng;
use rand::TryRngCore;
use base64::{engine::general_purpose::STANDARD, Engine as _};

#[derive(Debug, Serialize, Deserialize)]
struct Config {
    mysql: MySQL,
}

#[derive(Debug, Serialize, Deserialize)]
struct MySQL {
    user: String,
    password: String,
    host: String,
    port: u16,
}

pub fn create_database(db_name:&str) {
    // open json file
    let file = File::open("../settings.json").unwrap();
    let reader = BufReader::new(file);

    // Parse JSON into struct
    let config: Config = from_reader(reader).expect("Failed to parse JSON");
    // outputs a String. NOT a &str.
    let url = format!(
        "mysql://{}:{}@{}:{}",
        config.mysql.user, config.mysql.password, config.mysql.host, config.mysql.port
    );
    // doesn't work for String types we need &str
    let pool = Pool::new(url.as_ref()).unwrap();
    let mut conn = pool.get_conn().unwrap();

    let db_query: String = format!("CREATE DATABASE {}", db_name);
    match conn.query_drop(db_query){
        Ok(_) => {},
        Err(e) => {
            if e.to_string().contains("database exists") {
                // do nothing, continue
                // println!("Database {} already exists. Ignoring error.", db_name);
            } else {
                // throw irrecoverable error
                panic!("{}",e);
            }
        }
    }
}

pub fn create_users_table(db_name:&str) {
    // open json file
    let file = File::open("../settings.json").unwrap();
    let reader = BufReader::new(file);

    // Parse JSON into struct
    let config: Config = from_reader(reader).expect("Failed to parse JSON");
    // outputs a String. NOT a &str.
    let url = format!(
        "mysql://{}:{}@{}:{}/{}",
        config.mysql.user, config.mysql.password, config.mysql.host, config.mysql.port, db_name
    );
    // doesn't work for String types we need &str
    let pool = Pool::new(url.as_ref()).unwrap();
    let mut conn = pool.get_conn().unwrap();

    // Example query
    let db_query: &str = "CREATE TABLE `users` (`username` varchar(50) 
        NOT NULL,`password` varchar(128) NOT NULL, `salt` varchar(128) NOT NULL, PRIMARY KEY (`username`)) ENGINE=InnoDB";
    match conn.query_drop(db_query){
        // do nothig on success
        Ok(_) => {},
        Err(e) => {
            // ignore table already exists error but throw every other error
            if e.to_string().contains("Table 'users' already exists") {
                // println!("Users table already exists in database. Ignoring Error");
            } else {
                panic!("{}",e);
            }
        }
    }
}

pub fn create_account(username:&str, password:&str){
    // first salt and hash the password
    // create unsalted password hash
    let mut password_hasher = Sha3_512::new();
    password_hasher.update(password);
    let unsalted_hash = password_hasher.finalize();
    
    // create random 16 byte salt
    let mut salt = [0u8; 16];
    OsRng.try_fill_bytes(&mut salt).unwrap();

    // concatenate the hashed password with the salt and hash again
    let mut combined = [0u8; 80]; 
    // fixed size array because we know the salt is 18 bytes + hash is always 64 bytes
    combined[..64].copy_from_slice(&unsalted_hash);
    combined[64..].copy_from_slice(&salt);

    let mut salted_hasher = Sha3_512::new();
    salted_hasher.update(combined);
    let salted_hash = salted_hasher.finalize();
    // upload salt to DB
    // open json config file
    let file = File::open("../settings.json").unwrap();
    let reader = BufReader::new(file);

    // Parse JSON into struct
    let config: Config = from_reader(reader).expect("Failed to parse JSON");
    // outputs a String. NOT a &str.
    let url = format!(
        "mysql://{}:{}@{}:{}/masters_project_users",
        config.mysql.user, config.mysql.password, config.mysql.host, config.mysql.port
    );
    // doesn't work for String types we need &str
    let pool = Pool::new(url.as_ref()).unwrap();
    let mut conn = pool.get_conn().unwrap();

    let salt_stmnt = conn.prep("INSERT INTO users (username, password, salt) VALUES (?, ?, ?)").unwrap();
    let _ = conn.exec_drop(&salt_stmnt, (username, STANDARD.encode(salted_hash), STANDARD.encode(salt))); // encode hashed password into base64
}