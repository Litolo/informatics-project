mod accounts;
mod authentication;
mod authorisation;
use std::time::Instant;
use std::fs::{OpenOptions};
use std::io::{Write};

use rand::{rng, Rng};
use rand::distr::Alphanumeric; // need these for random naming for execution times file to avoid overwrites

fn main() {
    // set up the databases schema.
    accounts::create_database("masters_project_users");
    accounts::create_users_table("masters_project_users");
    // create two accounts, Alice and Bob
    accounts::create_account("Alice", "Alice123");
    accounts::create_account("Bob", "Bob123");
    accounts::create_account("/etc/passwords", "password"); // try to perform path traversal injection

    let _ = authorisation::create_post("", "I am up to no good", "/etc/passwords", "password"); //ignore errors for now

    let _ = authorisation::create_post("My First Post", "This is a sample message to demonstrate that Alice can create posts",
                                "Alice", "Alice123"); //ignore errors for now
    let content = authorisation::read_post("My First Post","Alice","Bob","Bob123"); //ignore errors for now
    println!("{}", content.unwrap());
    // read a post with the wrong credentials to login
    match authorisation::read_post("My First Post","Alice","Bob","b"){
        Ok(content) => {println!("{}",content)},
        Err(e) => {
            // we catch the "Invalid username or password" and user account does not exist errors
            // all other errors we give a general message so we don't give potentially harmful information away.
            if e.to_string().contains("Invalid username or password") ||
            e.to_string().contains("User account with username does not exist: Bob"){
                println!("Login failed. Invalid username or password.");
            }
        }
    }
    // try to read a post with an account that doesn't exist
    match authorisation::read_post("My First Post","Alice","Charlie","Charlie123"){
        Ok(content) => {println!("{}",content)},
        Err(e) => {
            // we catch the "Invalid username or password" and user account does not exist errors
            // all other errors we give a general message so we don't give potentially harmful information away.
            if e.to_string().contains("Invalid username or password") ||
                e.to_string().contains("User account with username does not exist: Charlie") {
                    println!("Login failed. Invalid username or password.");
            }
        }
    }
    // analysis part
    let mut create_db_times = Vec::new();
    let mut create_table_times = Vec::new();
    let mut create_account_times = Vec::new();
    let mut create_post_times = Vec::new();
    let mut read_post_times = Vec::new();


    for i in 0..100 {
        let str_i = format!("{}",i);
        let start_time = Instant::now();
        accounts::create_database("masters_project_users");
        create_db_times.push(start_time.elapsed().as_nanos());

        let start_time = Instant::now();
        accounts::create_users_table("masters_project_users");
        create_table_times.push(start_time.elapsed().as_nanos());

        let start_time = Instant::now();
        accounts::create_account(&str_i, &str_i);
        create_account_times.push(start_time.elapsed().as_nanos());

        let start_time = Instant::now();
        let _ = authorisation::create_post("Post", "Content", &str_i, &str_i);
        create_post_times.push(start_time.elapsed().as_nanos());

        let start_time = Instant::now();
        let _ = authorisation::read_post("Post", &str_i, &str_i, &str_i);
        read_post_times.push(start_time.elapsed().as_nanos());

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
        .open(format!("execution_times_auth_rust_{}.csv", rand_string))
        .unwrap();

    // Write the header
    writeln!(file, "createDB,createTable,createAccount,createPost,readPost").unwrap();

    // Write the data
    for i in 0..100 {
        writeln!(
            file,
            "{},{},{},{},{}",
            create_db_times[i], create_table_times[i], create_account_times[i], create_post_times[i], read_post_times[i]
        )
        .unwrap();
    }
}
