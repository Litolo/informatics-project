import accounts
import authorisation
import csv
import time

import string
import random # used to generate unique csv file name
createDB_times = []
createTable_times = []
createAccount_times = []
createPost_times = []
readPost_times = []


# Set up the MySQL database
accounts.create_database('masters_project_users')
accounts.create_users_table('masters_project_users')

# make an account for Alice and Bob
accounts.create_account('Alice', 'Alice123')
accounts.create_account('Bob','Bob123')

authorisation.create_post("My First Post", "This is a sample message to demonstrate that Alice can create posts",
                          "Alice", "Alice123")
content = authorisation.read_post("My First Post","Alice","Bob","Bob123")
print(content)

# try to do some path traversal
accounts.create_account('/etc/passwords', 'password')
authorisation.create_post("../../../../etc/passwords", "I am up to no good", "Alice", "password")

# do an analysis for running times
for i in range(100):
    # make db
    start_time = time.time_ns()
    accounts.create_database('masters_project_users') # this will already exist but we can test the running time anyway
    createDB_times.append(time.time_ns() - start_time)

    # make table
    start_time = time.time_ns()
    accounts.create_users_table('masters_project_users')
    createTable_times.append(time.time_ns() - start_time)

    # Measure account create
    start_time = time.time_ns()
    accounts.create_account(str(i), str(i))
    createAccount_times.append(time.time_ns() - start_time)

    # measure post create/read
    start_time = time.time_ns()
    authorisation.create_post("Post", "Content",str(i), str(i))
    createPost_times.append(time.time_ns() - start_time)

    start_time = time.time_ns()
    authorisation.read_post("Post",str(i),"Bob","Bob123")
    readPost_times.append(time.time_ns() - start_time)

rand_string = ''.join(random.choices(string.ascii_letters + string.digits, k=30))

with open("execution_times_auth_python_{}.csv".format(rand_string), "w", newline="") as csvfile:
    writer = csv.writer(csvfile)
    
    # Write the header row
    writer.writerow([
        "createDB",
        "createTable",
        "createAccount",
        "createPost",
        "readPost"
    ])
    
    # Write the data rows
    for i in range(100):
        writer.writerow([
            createDB_times[i],
            createPost_times[i],
            createAccount_times[i],
            createPost_times[i],
            readPost_times[i]
        ])