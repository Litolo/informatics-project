# Informatics Project – Maintenance Manual

This project demonstrates secure software engineering practices using three programming languages (Python, Java, and Rust) across different use cases.

**GitHub Repository**: [https://github.com/Litolo/informatics-project](https://github.com/Litolo/informatics-project)

---

## 📦 Installation

Clone the repository at [https://github.com/Litolo/informatics-project](https://github.com/Litolo/informatics-project)

---

## 🔧 Requirements

### Core Dependencies
- [Python](https://www.python.org/) ≥ 3.12  
- [Java Development Kit (JDK)](https://www.oracle.com/java/technologies/downloads/) ≥ 21  
- [Maven](https://maven.apache.org/what-is-maven.html) ≥ 3.8  
- [Rust](https://www.rust-lang.org/) ≥ 1.86  

> ⚠️ Source programs **may** work with lower versions, but compatibility is not guaranteed.

### Optional Dependencies (Use-Case Specific)
- **Use Case 1 (Email Encryption)**: [Docker](https://www.docker.com/), [Docker Compose](https://docs.docker.com/compose/install/)  
- **Use Case 2 (Authentication Blog Posts)**: [MySQL](https://www.mysql.com/)

---

## ⚙️ Configuration

### .env Secrets File
Sensitive credentials should be placed in a `.env` file, see [Masters_project/encryption/.env](Masters_project/encryption/.env):
```env
ALICE_KEY_PASSWORD=AliceKey123
BOB_KEY_PASSWORD=BobKey123
BOB_EMAIL_PASSWORD=BobEmail123
ALICE_EMAIL_PASSWORD=AliceEmail123
```

### SMTP Configuration
Edit [Masters_project/encryption/settings.json](Masters_project/encryption/settings.json) with your SMTP server details.  
To run a local test SMTP server:

```bash
docker compose up
# OR
docker run --rm -it -p 5000:80 -p 2525:25 rnwood/smtp4dev
```

### MySQL Configuration
Edit [Masters project/authentication/settings.json](Masters_project/authentication/settings.json):
```json
{
  "mysql": {
    "user": "root",
    "password": "super_secure_pAssw0rd123",
    "host": "127.0.0.1",
    "port": 3306
  }
}
```
> ⚠️ Do not use the root user in production. Create a user with limited privileges.

---

## 💾 Space & Memory

- **Storage**: ~330MB including optional dependencies  
- **Memory**: No hard minimum, but performance may vary on low-memory systems

---

## 🚀 Running the Use-Cases

### Python
Navigate to the relevant folder and install dependencies:
```bash
pip install -r requirements.txt
```

Run:
- Use Case 1 & 2: `main.py`
- Use Case 3: Run `server.py` and `client.py` in separate terminals

### Java
Navigate to the relevant Maven directory and run:
```bash
mvn exec:java
```

Use Case 3 (RPS):
```bash
# Server
mvn exec:java -Dexec.mainClass="com.github.Litolo.RPS.Server"

# Client
mvn exec:java -Dexec.mainClass="com.github.Litolo.RPS.Client"
```

---

### Rust
Navigate to the relevant folder and run:
- Use Case 1 & 2: `cargo build && cargo run`
- Use Case 3:
```bash
# Server
rustc server.rs -o server && ./server

# Client
rustc client.rs -o client && ./client 
```

## 📁 Project Structure Highlights

```
masters project/
├── encryption/               # Use Case 1 - Email Encryption
│   ├── java/keys/            # PEM files and certificates
│   ├── python/
│   ├── rust/
│   ├── .env
│   ├── docker-compose.yml    # Local SMTP config
│   ├── settings.json         # SMTP settings
├── authentication/           # Use Case 2 - Blog Authentication
│   ├── java/posts/           # Blog post files
│   ├── python/
│   ├── rust/
│   ├── settings.json         # MySQL settings
├── concurrency/              # Use Case 3 - RPS Game
│   ├── java/
│   ├── python/
│   ├── rust/
```

---

## 🔮 Future Improvements

- **Language Support**: Add Go, C#, C/C++  
- **Testing**: Add fuzz testing, code coverage, stronger automation  
- **Security**: Implement multi-factor authentication (TOTP, email, biometrics)

---
