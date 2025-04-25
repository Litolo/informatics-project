use openssl::x509::{X509, X509VerifyResult};
use openssl::stack::Stack;
use openssl::ssl::{SslContextBuilder, SslMethod};
use std::time::{SystemTime, UNIX_EPOCH};

pub fn verify_chain(
    cert: &X509,
    intermediates: &Stack<X509>,
    trusted_certs: &Stack<X509>,
) -> Result<(), Box<dyn std::error::Error>> {
    // Create a new SSL context
    let mut ctx_builder = SslContextBuilder::new(SslMethod::tls()).unwrap();
    
    // Add trusted certificates
    let ctx = ctx_builder
        .set_verify_callback(openssl::ssl::SslVerifyMode::PEER, |_, _| true);
    
    let store = ctx.cert_store_mut();
    for trusted_cert in trusted_certs {
        store.add_cert(trusted_cert.to_owned()).unwrap();
    }

    // Verify the certificate chain
    let mut store_ctx = openssl::x509::store::X509StoreContext::new().unwrap();
    let verified = store_ctx.init(
        store,
        cert,
        intermediates,
        |ctx| ctx.verify_cert()
    ).unwrap();

    if verified {
        Ok(())
    } else {
        let error = store_ctx.error();
        Err(format!("Certificate chain verification failed: {}", error).into())
    }
}

pub fn verify_attributes(cert: &X509) -> Result<(), Box<dyn std::error::Error>> {
    let current_time = SystemTime::now()
        .duration_since(UNIX_EPOCH)?
        .as_secs();

    let not_before = cert.not_before().to_unix()?;
    let not_after = cert.not_after().to_unix()?;

    if current_time < not_before {
        return Err(format!(
            "Certificate is not yet valid (valid from: {})",
            cert.not_before()
        ).into());
    }

    if current_time > not_after {
        return Err(format!(
            "Certificate has expired (expired on: {})",
            cert.not_after()
        ).into());
    }

    Ok(())
}