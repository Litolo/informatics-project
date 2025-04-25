
from mysql.connector.errors import DatabaseError
import mysql.connector
import config
from cryptography.hazmat.primitives import hashes
import os
from base64 import b64encode

def create_database(DB_name):
    # its good practice not to connect to a database as root! 
    # Make sure you create an account for this service and restrict its permissions
    # if you are intending to use this in a production setting
    cnx = mysql.connector.connect(**config.get("mysql"))
    cursor = cnx.cursor() 
    try:
        cursor.execute("CREATE DATABASE {}".format(DB_name))
        # print("Database created successfully")

    except DatabaseError as e:
        pass # do nothing
        # print(e)
        # print("Continuing...")
    cursor.close()
    cnx.close()
    return

def create_users_table(DB_name):
    users_schema = (
        "CREATE TABLE `users` ("
        "  `username` varchar(50) NOT NULL,"
        "  `password` varchar(128) NOT NULL,"
        "  `salt` varchar(128) NOT NULL,"
        "  PRIMARY KEY (`username`)"
        ") ENGINE=InnoDB")
    cnx = mysql.connector.connect(**config.get("mysql"))
    cnx.database = DB_name
    cursor = cnx.cursor() 
    try:
        cursor.execute(users_schema)
        # print("Users table created successfully")
    except DatabaseError as e:
        pass # do nothing
        # print(e)
        # print("Continuing...")
    cursor.close()
    cnx.close()
    return

def create_account(username, password):
    # add some simple username/password restrictions

    # things to be aware of:
    # 1 - we DO NOT trust user input. we need to escape strings so that injection is impossible
    # 2 - plaintext passwords are insecure. We need to hash and salt them
    prepared_SQL = ("INSERT INTO users "
               "(username, password, salt) "
               "VALUES (%s, %s, %s)")
    # use the newly approved SHA3_512 hash function
    # https://csrc.nist.gov/projects/hash-functions
    # it is slower than SHA2 hash functions, but is likely to remain secure if SHA2 functions are cracked
    unsalted_digest = hashes.Hash(hashes.SHA3_512())
    # generate random 16 bytes salt
    # we want to store in the database - hash(hash(password) + salt)
    salt = os.urandom(16)
    encoded_salt = b64encode(salt)

    unsalted_digest.update(password.encode())
    unsalted_byte_digest = unsalted_digest.finalize()

    salted_digest = hashes.Hash(hashes.SHA3_512())
    salted_digest.update(unsalted_byte_digest + salt)
    salted_byte_digest = salted_digest.finalize()
    
    user_tuple = (username, salted_byte_digest.hex(), encoded_salt)
    cnx = mysql.connector.connect(**config.get("mysql"))
    cnx.database = "masters_project_users"
    cursor = cnx.cursor()
    # will throw an integrity error if username is already in use. Handle this gracefully in the main loop.
    cursor.execute(prepared_SQL, user_tuple)
    # commit and close DB connection
    cnx.commit()
    cursor.close()
    cnx.close()
    # print("Account created successfully for {}".format(username))
    
    return

