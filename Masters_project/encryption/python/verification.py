from cryptography.x509 import Certificate, DNSName, load_pem_x509_certificates
from cryptography.x509.verification import PolicyBuilder, Store
from datetime import datetime

def verifyChain(certificate, intermediaries, trusted_certs):
    store = Store(trusted_certs)
    builder = PolicyBuilder().store(store)
    verifier = builder.build_server_verifier(DNSName("cryptography.io"))
    chain = verifier.verify(certificate, intermediaries)
    if len(chain) > 0:
        return True

def verifyAttributes(cert):
    # returns True if valid, ValueError if the certificate is expired or not yet valid
    current_time = datetime.now()
    if cert.not_valid_before > current_time:
        raise ValueError("Certificate is not yet valid (valid from: {})".format(cert.not_valid_before))
    if cert.not_valid_after < current_time:
        raise ValueError("Certificate has expired (expired on: {})".format(cert.not_valid_after))
    return True