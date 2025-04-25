from mysql.connector.errors import DatabaseError
import mysql.connector
import config
from cryptography.hazmat.primitives import hashes
from base64 import b64decode

def login(db_name, username, password):
    # its good practice not to connect to a database as root! 
    # Make sure you create an account for this service and restrict its permissions
    # if you are intending to use this in a production setting
    
    # read the salt from the mysql DB
    cnx = mysql.connector.connect(**config.get("mysql"), database=db_name)
    cursor = cnx.cursor()
    query = "SELECT salt FROM users WHERE username = %s LIMIT 0, 1"
    cursor.execute(query, (username,))
    row = cursor.fetchone()
    b64encoded_salt = row[0]
    
    # recreate the hashed password based on the password string
    unsalted_digest = hashes.Hash(hashes.SHA3_512())

    unsalted_digest.update(password.encode())
    unsalted_byte_digest = unsalted_digest.finalize()
    
    salted_digest = hashes.Hash(hashes.SHA3_512())
    salted_digest.update(unsalted_byte_digest + b64decode(b64encoded_salt))
    salted_byte_digest = salted_digest.finalize()

    query = ("SELECT * FROM users "
        "WHERE username = %s AND password = %s LIMIT 0, 1")

    cursor.execute(query, (username, salted_byte_digest.hex()))
    cursor.fetchall() # even if we dont do anything with this we have to fetch the result or we get an error
    cursor.close()
    cnx.close()
    return True