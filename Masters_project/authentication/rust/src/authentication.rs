use serde::{Deserialize, Serialize};
use std::fs::File;
use std::io::{BufReader};
use serde_json::from_reader;
use mysql::*;
use mysql::prelude::*;
use sha3::{Sha3_512, Digest};
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
// the function returns the result of the SELECT statement on the users table 
// only if the username AND password is correct. Otherwise, returns None.
// requires better error descriptions and reporting.
pub fn login(db_name:&str, username:&str, password:&str) -> Result<Option<(String, String, String)>, String>{
    // first load the salt, then check the users DB
    // open json config file
    let file = File::open("../settings.json").expect("Failed to open config file");
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

    let salt_stmnt = conn.prep("SELECT salt FROM users WHERE username = ? LIMIT 0, 1").unwrap();
    let res_salt: Option<String> = conn.exec_first(&salt_stmnt, (username,)).unwrap();
    // this is the first point of failure for the case where an account with the given username does not exist
    if res_salt == None{
        Err(String::from(format!("User account with username does not exist: {}",username)))
    }
    else {
        let salt = STANDARD.decode(res_salt.unwrap()).unwrap();

        // create unsalted password hash
        let mut password_hasher = Sha3_512::new();
        password_hasher.update(password);
        let unsalted_hash = password_hasher.finalize();
        // concatenate the hashed password with the salt and hash again
        let mut combined = [0u8; 80]; 
        // fixed size array because we know the salt is 18 bytes + hash is always 64 bytes
        combined[..64].copy_from_slice(&unsalted_hash);
        combined[64..].copy_from_slice(&salt);

        let mut salted_hasher = Sha3_512::new();
        salted_hasher.update(combined);
        let salted_hash = salted_hasher.finalize();

        let salt_stmnt = conn.prep("SELECT * FROM users WHERE username = ? AND password = ? LIMIT 0, 1").unwrap();
        let res_user: Option<(String, String, String)> = conn.exec_first(&salt_stmnt, (username, STANDARD.encode(salted_hash))).unwrap();
        Ok(res_user) // returns None if query result is empty (username or password incorrect)
    }
}