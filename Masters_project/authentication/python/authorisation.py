import authentication
import os

def sanitise_filename(part: str) -> str:
    # Replace slashes, backslashes, and other risky characters to prevent CWE-22
    # This may limit user "expression" with their titles and usernames but it can be converted again later
    return part.replace("/", "%2F").replace("\\", "%5C")

# restrict only those users to edit their own posts. 
# Do not allow users to access other users posts 
# eg. for user 'Alice' only allow her to write to the posts/Alice/ directory
def create_post(title:str, content:str, username:str, password:str):
    res = None
    res = authentication.login("masters_project_users", username, password)
    try:
        res = authentication.login("masters_project_users", username, password)
    except:
        print("Failed to login")
    if res==False:
        print("Incorrect username or password")
        return
    # else account successfully authenticated. Allow them to create or edit post
    # but only add to their own directory i.e. posts/{username}/
    else:
        # Sanitize inputs
        safe_username = sanitise_filename(username)
        safe_title = sanitise_filename(title)
        
        # Define the base directory and validate the path
        base_dir = os.path.abspath("../posts/")
        filename = f"{safe_username}_{safe_title}.txt"
        full_path = os.path.join(base_dir, filename)

        # Ensure the path stays within the base directory -> final line of defence
        if not os.path.abspath(full_path).startswith(base_dir):
            raise ValueError("Invalid filename: path traversal detected")

        with open(full_path, "w") as f:
            f.write(content)

def read_post(title:str, target_user:str, username:str, password:str):
    # read a post from the target user. Requires a valid account.
    res = None
    res = authentication.login("masters_project_users", username, password)
    try:
        res = authentication.login("masters_project_users", username, password)
    except:
        print("Failed to login")
    if res==False:
        print("Incorrect username or password")
        return
    else:
         # Sanitize inputs
        safe_target_user = sanitise_filename(target_user)
        safe_title = sanitise_filename(title)
        
        # Validate path
        base_dir = os.path.abspath("../posts/")
        filename = f"{safe_target_user}_{safe_title}.txt"
        full_path = os.path.join(base_dir, filename)

        if not os.path.abspath(full_path).startswith(base_dir):
            raise ValueError("Invalid filename: path traversal detected")

        with open(full_path, "r") as f:
            return f.read()