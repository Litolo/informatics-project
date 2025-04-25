use openssl::rsa::Rsa;
use openssl::pkey::PKey;
use openssl::x509::{X509, X509NameBuilder, X509Builder};
use openssl::x509::extension::{BasicConstraints, KeyUsage, ExtendedKeyUsage, SubjectAlternativeName, SubjectKeyIdentifier, AuthorityKeyIdentifier};
use openssl::hash::MessageDigest;
use openssl::asn1::Asn1Time;
use openssl::symm::Cipher;
use std::fs;

pub fn make_keys() {
    // Generate RSA key pair
    let rsa = Rsa::generate(2048).unwrap();
    let pkey = PKey::from_rsa(rsa).unwrap();

    // Save private key with password protection
    let priv_key: Vec<u8> = pkey.private_key_to_pem_pkcs8_passphrase(Cipher::aes_128_cbc(), b"Bob123").unwrap();
    fs::write("private_key.pem", priv_key).unwrap();
    
    // Save public key
    let pub_key: Vec<u8> = pkey.public_key_to_pem().unwrap();
    fs::write("public_key.pem", pub_key).unwrap();

    // Build subject name
    let mut x509_name = X509NameBuilder::new().unwrap();
    x509_name.append_entry_by_text("C", "US").unwrap();
    x509_name.append_entry_by_text("ST", "California").unwrap();
    x509_name.append_entry_by_text("L", "San Francisco").unwrap();
    x509_name.append_entry_by_text("O", "My Company").unwrap();
    let name = x509_name.build();

    // Create certificate builder
    let mut builder = X509Builder::new().unwrap();
    builder.set_version(2).unwrap(); // X.509 v3
    builder.set_subject_name(&name).unwrap();
    builder.set_issuer_name(&name).unwrap(); // Self-signed
    builder.set_pubkey(&pkey).unwrap();

    // Set validity period (10 days like Python version)
    builder.set_not_before(Asn1Time::days_from_now(0).unwrap().as_ref()).unwrap();
    builder.set_not_after(Asn1Time::days_from_now(10).unwrap().as_ref()).unwrap();
    
    // Subject Alternative Name
    let san = SubjectAlternativeName::new()
        .dns("cryptography.io")
        .dns("www.cryptography.io")
        .build(&builder.x509v3_context(None, None))
        .unwrap();
    builder.append_extension(san).unwrap();
    
    // Basic Constraints (CA:false)
    let bc = BasicConstraints::new()
        .critical()
        .ca()
        .build()
        .unwrap();
    builder.append_extension(bc).unwrap();
    
    // Key Usage
    let ku = KeyUsage::new()
        .critical()
        .digital_signature()
        .key_encipherment()
        .crl_sign()
        .build()
        .unwrap();
    builder.append_extension(ku).unwrap();
    
    // Extended Key Usage
    let eku = ExtendedKeyUsage::new()
        .server_auth()
        .client_auth()
        .build()
        .unwrap();
    builder.append_extension(eku).unwrap();
    
    // Subject Key Identifier
    let skid = SubjectKeyIdentifier::new()
        .build(&builder.x509v3_context(None, None))
        .unwrap();
    builder.append_extension(skid).unwrap();
    
    // Authority Key Identifier (since this is self-signed, it points to itself)
    let akid = AuthorityKeyIdentifier::new()
        .keyid(true)
        .build(&builder.x509v3_context(None, None))
        .unwrap();
    builder.append_extension(akid).unwrap();

    // Sign the certificate
    builder.sign(&pkey, MessageDigest::sha256()).unwrap();

    // Build and save the certificate
    let certificate: X509 = builder.build();
    fs::write("certificate.pem", certificate.to_pem().unwrap()).unwrap();
}
