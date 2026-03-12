# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in LifeModule — especially related to database encryption, backup handling, or data integrity — please report it responsibly.

**Email:** contact@lifemodule.de

Please do **not** open a public GitHub issue for security vulnerabilities. I will respond within 7 days and work on a fix.

## Scope

LifeModule stores sensitive data locally (health metrics, mood, vehicle logs). Security-relevant areas include:

- SQLCipher database encryption and key management
- Backup file encryption/decryption (`.lmbackup`)
- Hash chain integrity (logbook, scanner)
- Health Connect data handling
