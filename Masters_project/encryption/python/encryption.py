# Import smtplib for the actual sending function
import smtplib
# Import the email modules we'll need
from email.message import EmailMessage
from email.policy import SMTP
from cryptography import x509
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding
from base64 import b64encode
import config

def encryptMsg(cert_path:str, plaintext_path:str, _from:str, _to:str, _password:str, _subject:str="No Subject Set"):
    # load the public certificate
    with open(cert_path, 'rb') as fp:
        data = fp.read()
    cert = x509.load_pem_x509_certificate(data)
    pub_key = cert.public_key()

    # Open the plain text email message file
    with open(plaintext_path, "rb") as fp:
        # Create a text/plain message
        msg = EmailMessage()
        content = fp.read()
        ciphertext = pub_key.encrypt(
            content,
            padding.OAEP(
            mgf=padding.MGF1(algorithm=hashes.SHA512()),
            algorithm=hashes.SHA512(),
            label=None
            )
        )
        #convert ciphertext bytes to human readable string so it can be sent by email
        temp = b64encode(ciphertext).decode('utf-8')
        msg.set_content(temp)

    msg['Subject'] = _subject
    msg['From'] = _from
    msg['To'] = _to

    smtp = config.get("smtp")
    # Send the message via our own SMTP server using TLS and authenticate
    with smtplib.SMTP_SSL(smtp["host"]+":"+smtp["ports"]["smtp"]) as server:
        server.login(_from, _password)
        server.send_message(msg)
        server.quit()

    with open(_from+"_email.eml", 'wb') as fp:
        fp.write(msg.as_bytes(policy=SMTP))
    return