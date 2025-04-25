# Import the email modules we'll need
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding
from base64 import b64decode
from cryptography.hazmat.primitives import serialization
import email
from email.policy import SMTP

def decryptEmail(bytes_email_path:str, priv_key_path:str, password):
    with open(bytes_email_path, 'rb') as fp:
        msg = email.message_from_binary_file(fp, policy=SMTP)

    ciphertext = msg.get_payload(decode=True)
    with open(priv_key_path, "rb") as key_file:
        private_key = serialization.load_pem_private_key(
            key_file.read(),
            password=password,
        )
        plaintext = private_key.decrypt(
        b64decode(ciphertext),
        padding.OAEP(
            mgf=padding.MGF1(algorithm=hashes.SHA512()),
            algorithm=hashes.SHA512(),
            label=None
        )
    )
    return plaintext.decode()