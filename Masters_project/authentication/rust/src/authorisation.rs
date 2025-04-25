use crate::authentication;
use std::fs;
use std::fs::File;
use std::io::Read;
use std::path::Path;

fn sanitise_filename(part: &str) -> String {
    part
        .replace("/", "%2F")    // Encode forward slash
        .replace("\\", "%5C")   // Encode backslash
}

pub fn read_post(title:&str, target_user:&str, username:&str, password:&str) -> Result<String, String> {
    // will panic if username does not exist
    // will return custom error if account exists but password is incorrect
    // pass any errors on as the return value in Result<_, Err> which happen in login function
    match authentication::login("masters_project_users", username, password){
        Ok(res) => {
            if res == None {
                Err(String::from("Login failed. Invalid username or password.")) // return an error
            }
            else {
                // Sanitize inputs
                let safe_target_user = sanitise_filename(target_user);
                let safe_title = sanitise_filename(title);

                // Define base directory
                let base_dir = Path::new("../posts").canonicalize().map_err(|e| e.to_string())?;
                let full_path = base_dir.join(format!("{}_{}.txt", safe_target_user, safe_title));

                // Validate path
                if !full_path.starts_with(&base_dir) {
                    return Err(String::from("Path traversal attempt detected"));
                }

                // Read file
                let mut f = File::open(&full_path).map_err(|e| e.to_string())?;
                let mut content = String::new();
                f.read_to_string(&mut content).map_err(|e| e.to_string())?;
                Ok(content)
            }
        }
        Err(e) => {
            // println!{"{}",e}; 
            if !e.to_string().contains("User account with username does not exist:"){
                panic!("{}",e)
            }
            else { Err(e) }
        }
    }
}

pub fn create_post(title:&str, content:&str, username:&str, password:&str) -> Result<(), String> {
    // will panic if username does not exist
    // will return custom error if account exists but password is incorrect
    // pass any errors on as the return value in Result<_, Err> which happen in login function
    match authentication::login("masters_project_users", username, password){
        Ok(res) => {
            if res == None {
                Err(String::from("Login failed. Invalid username or password."))
            }
            else {
                // Sanitize inputs
                let safe_username = sanitise_filename(username);
                let safe_title = sanitise_filename(title);

                // Define base directory
                let base_dir = Path::new("../posts").canonicalize().map_err(|e| e.to_string())?;
                let full_path = base_dir.join(format!("{}_{}.txt", safe_username, safe_title));

                // Validate path
                if !full_path.starts_with(&base_dir) {
                    return Err(String::from("Path traversal attempt detected"));
                }

                // Write file
                fs::write(&full_path, content).map_err(|e| e.to_string())?;
                Ok(())
            }
        }
        Err(e) => {
            // println!{"{}",e}; 
            if !e.to_string().contains("User account with username does not exist:"){
                panic!("{}",e)
            }
            else { Err(e) }
        }
    }
}