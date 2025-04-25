from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import hashes

import datetime

def makeRoot():
    root_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048)
    subject = issuer = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, "US"),
        x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, "California"),
        x509.NameAttribute(NameOID.LOCALITY_NAME, "San Francisco"),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, "My Company"),
        x509.NameAttribute(NameOID.COMMON_NAME, "PyCA Docs Root CA"),
    ])
    root_cert = x509.CertificateBuilder().subject_name(
        subject
    ).issuer_name(
        issuer
    ).public_key(
        root_key.public_key()
    ).serial_number(
        x509.random_serial_number()
    ).not_valid_before(
        datetime.datetime.now(datetime.timezone.utc)
    ).not_valid_after(
        # Our certificate will be valid for ~10 years
        datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=365*10)
    ).add_extension(
        x509.BasicConstraints(ca=True, path_length=None),
        critical=True,
    ).add_extension(
        x509.KeyUsage(
            digital_signature=True,
            content_commitment=False,
            key_encipherment=False,
            data_encipherment=False,
            key_agreement=False,
            key_cert_sign=True,
            crl_sign=True,
            encipher_only=False,
            decipher_only=False,
        ),
        critical=True,
    ).add_extension(
        x509.SubjectKeyIdentifier.from_public_key(root_key.public_key()),
        critical=False,
    ).sign(root_key, hashes.SHA256())
    with open("../keys/root_certificate.pem", "wb") as f:
        f.write(root_cert.public_bytes(serialization.Encoding.PEM))
    return root_key, root_cert

def makeIntermediary(root_key, root_cert, identifier):
    int_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048)
    subject = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, "US"),
        x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, "California"),
        x509.NameAttribute(NameOID.LOCALITY_NAME, "San Francisco"),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, "My Company"),
        x509.NameAttribute(NameOID.COMMON_NAME, "PyCA Docs Intermediate CA"),
    ])
    int_cert = x509.CertificateBuilder().subject_name(
        subject
    ).issuer_name(
        root_cert.subject
    ).public_key(
        int_key.public_key()
    ).serial_number(
        x509.random_serial_number()
    ).not_valid_before(
        datetime.datetime.now(datetime.timezone.utc)
    ).not_valid_after(
        # Our intermediate will be valid for ~3 years
        datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=365*3)
    ).add_extension(
        # Allow no further intermediates (path length 0)
        x509.BasicConstraints(ca=True, path_length=0),
        critical=True,
    ).add_extension(
        x509.KeyUsage(
            digital_signature=True,
            content_commitment=False,
            key_encipherment=False,
            data_encipherment=False,
            key_agreement=False,
            key_cert_sign=True,
            crl_sign=True,
            encipher_only=False,
            decipher_only=False,
        ),
        critical=True,
    ).add_extension(
        x509.SubjectKeyIdentifier.from_public_key(int_key.public_key()),
        critical=False,
    ).add_extension(
        x509.AuthorityKeyIdentifier.from_issuer_subject_key_identifier(
            root_cert.extensions.get_extension_for_class(x509.SubjectKeyIdentifier).value
        ),
        critical=False,
    ).sign(root_key, hashes.SHA256())

    with open("../keys/"+identifier+"_certificate.pem", "wb") as f:
        f.write(int_cert.public_bytes(serialization.Encoding.PEM))
    return int_key, int_cert

def makeCert(int_key, int_cert, identifier:str, password:bytes):
    ee_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048)
    with open("../keys/"+identifier+"_private_key.pem", "wb") as f:
        f.write(ee_key.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.BestAvailableEncryption(password),
        ))
    subject = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, "US"),
        x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, "California"),
        x509.NameAttribute(NameOID.LOCALITY_NAME, "San Francisco"),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, "My Company"),
    ])
    ee_cert = x509.CertificateBuilder().subject_name(
        subject
    ).issuer_name(
        int_cert.subject
    ).public_key(
        ee_key.public_key()
    ).serial_number(
        x509.random_serial_number()
    ).not_valid_before(
        datetime.datetime.now(datetime.timezone.utc)
    ).not_valid_after(
        # Our cert will be valid for 10 days
        datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=10)
    ).add_extension(
        x509.SubjectAlternativeName([
            # Describe what sites we want this certificate for.
            x509.DNSName("cryptography.io"),
            x509.DNSName("www.cryptography.io"),
        ]),
        critical=False,
    ).add_extension(
        x509.BasicConstraints(ca=False, path_length=None),
        critical=True,
    ).add_extension(
        x509.KeyUsage(
            digital_signature=True,
            content_commitment=False,
            key_encipherment=True,
            data_encipherment=False,
            key_agreement=False,
            key_cert_sign=False,
            crl_sign=True,
            encipher_only=False,
            decipher_only=False,
        ),
        critical=True,
    ).add_extension(
        x509.ExtendedKeyUsage([
            x509.ExtendedKeyUsageOID.CLIENT_AUTH,
            x509.ExtendedKeyUsageOID.SERVER_AUTH,
        ]),
        critical=False,
    ).add_extension(
        x509.SubjectKeyIdentifier.from_public_key(ee_key.public_key()),
        critical=False,
    ).add_extension(
        x509.AuthorityKeyIdentifier.from_issuer_subject_key_identifier(
            int_cert.extensions.get_extension_for_class(x509.SubjectKeyIdentifier).value
        ),
        critical=False,
    ).sign(int_key, hashes.SHA256())
    with open("../keys/"+identifier+"_certificate.pem", "wb") as f:
        f.write(ee_cert.public_bytes(serialization.Encoding.PEM))
    return ee_key, ee_cert
