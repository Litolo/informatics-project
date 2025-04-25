import csv
from encryption import encryptMsg
from generate_keys import makeCert, makeRoot, makeIntermediary
from decryption import decryptEmail
from verification import verifyChain
from dotenv import dotenv_values
import time

import random
import string

makeCert_times = []
encryptMsg_times = []
decryptEmail_times = []
verifyChain_times = []

# get the secret passwords from env file
secrets = dotenv_values("../.env")

root_key, root_cert = makeRoot() # make the root CA
inter_key_1, inter_cert_1 = makeIntermediary(root_key, root_cert, "intermediary_1")
alice_key, alice_cert = makeCert(inter_key_1, inter_cert_1, "Alice", secrets["ALICE_KEY_PASSWORD"].encode()) # leaf CA, intermediary1 is signer (parent)
inter_key_2, inter_cert_2 = makeIntermediary(root_key, root_cert, "intermediary_2")
bob_key, bob_cert = makeCert(inter_key_2, inter_cert_2, "Bob", secrets["BOB_KEY_PASSWORD"].encode()) # leaf CA, intermediary2 is signer (parent)
# encrypt the plaintext message and send it to bob
encryptMsg('../keys/Bob_certificate.pem', '../Alice_email.txt', "Alice@email.com", "Bob@email.com", secrets["ALICE_EMAIL_PASSWORD"], "Sending sample to Bob")
# assume we have retrieved the email from our mailbox provider. 
# In this case we are working with the "downloaded" raw email
AliceToBob = decryptEmail('Alice@email.com_email.eml', "../keys/Bob_private_key.pem",secrets["BOB_KEY_PASSWORD"].encode())
with open('../Alice_email.txt') as f:
    AliceOriginal = f.read()
    f.close()
print("Does Alice's original email match the one Bob decrypted?: ", AliceToBob == AliceOriginal)

# now send one back to Alice
encryptMsg('../keys/Alice_certificate.pem', '../Bob_email.txt', "Bob@email.com", "Alice@email.com", secrets["BOB_EMAIL_PASSWORD"], "Replying to Alice")
BobToAlice = decryptEmail('Bob@email.com_email.eml', "../keys/Alice_private_key.pem",secrets["ALICE_KEY_PASSWORD"].encode())
with open('../Bob_email.txt') as f:
    BobOriginal = f.read()
    f.close()
print("Does Bob's original email match the one Alice decrypted?: ", BobToAlice == BobOriginal)

verifyChain(bob_cert, [inter_cert_2], [root_cert])

# do an analysis for running times
for _ in range(100):
     # Measure makeCert for Alice
    start_time = time.time_ns()
    alice_key, alice_cert = makeCert(inter_key_1, inter_cert_1, "Alice", secrets["ALICE_KEY_PASSWORD"].encode())
    makeCert_times.append(time.time_ns() - start_time)

    # Measure encryptMsg for Alice to Bob
    start_time = time.time_ns()
    encryptMsg('../keys/Bob_certificate.pem', '../Alice_email.txt', "Alice@email.com", "Bob@email.com", secrets["ALICE_EMAIL_PASSWORD"], "Sending sample to Bob")
    encryptMsg_times.append(time.time_ns() - start_time)

    # decryption
    start_time = time.time_ns()
    AliceToBob = decryptEmail('Alice@email.com_email.eml', "../keys/Bob_private_key.pem", secrets["BOB_KEY_PASSWORD"].encode())
    decryptEmail_times.append(time.time_ns() - start_time)

rand_string = ''.join(random.choices(string.ascii_letters + string.digits, k=30))

with open("execution_times_encryption_python_{}.csv".format(rand_string), "w", newline="") as csvfile:
    writer = csv.writer(csvfile)
    
    # Write the header row
    writer.writerow([
        "makeCert",
        "encryptMsg",
        "decryptEmail",
    ])
    
    # Write the data rows
    for i in range(100):
        writer.writerow([
            makeCert_times[i],
            encryptMsg_times[i],
            decryptEmail_times[i],
        ])