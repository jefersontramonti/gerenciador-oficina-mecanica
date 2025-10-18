# Security Policy - PitStop

## Overview

This document describes security best practices for developing, deploying, and maintaining the PitStop application.

## Table of Contents

- [Secrets Management](#secrets-management)
- [Configuration Files](#configuration-files)
- [Git Security](#git-security)
- [JWT Security](#jwt-security)
- [Database Security](#database-security)
- [Deployment Security](#deployment-security)

---

## Secrets Management

### NEVER Commit These Files

The following files contain sensitive data and **MUST NEVER** be committed to Git:

```
L src/main/resources/application-dev.yml
L src/main/resources/application-prod.yml
L src/main/resources/application-local.yml
L .env
L *.key, *.pem, *.p12, *.jks
L credentials.json, serviceAccountKey.json
```

These files are already listed in `.gitignore`. If you accidentally commit them, see [Git Security](#git-security) section.

### Template Files (Safe to Commit)

These files contain **FAKE credentials** as examples:

```
 .env.example
 src/main/resources/application-dev.yml.example
```

### How to Set Up Local Configuration

1. **Copy template files:**
   ```bash
   cp .env.example .env
   cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
   ```

2. **Fill in your real credentials:**
   - Open `.env` or `application-dev.yml`
   - Replace placeholder values with your actual credentials from:
     - Neon PostgreSQL: https://console.neon.tech/
     - JWT Secret: `http://localhost:8080/api/debug/generate-jwt-secret`
     - Other services as needed

3. **Verify files are ignored:**
   ```bash
   git status
   # application-dev.yml and .env should NOT appear in "Changes to be committed"
   ```

---

## Configuration Files

### application.yml (Public)

**Purpose:** Base configuration shared across all environments
**Sensitive Data:** L NO - Safe to commit
**What it contains:**
- Server port, compression, error handling
- Logging patterns
- JPA/Hibernate settings
- Actuator endpoints
- SpringDoc/Swagger configuration

**Environment variables references:**
```yaml
datasource:
  url: ${DATABASE_URL:}
  username: ${DATABASE_USERNAME:}
  password: ${DATABASE_PASSWORD:}
jwt:
  secret: ${JWT_SECRET}
```

These values are provided by `application-dev.yml` or environment variables in production.

### application-dev.yml (Private)

**Purpose:** Development-specific configuration with real credentials
**Sensitive Data:**  YES - NEVER commit
**What it contains:**
- Neon PostgreSQL connection string
- Database username and password
- JWT_SECRET (Base64-encoded 512-bit key)
- Redis host/port

**Location:** `src/main/resources/application-dev.yml`
**Status:** Already in `.gitignore`

### application-dev.yml.example (Template)

**Purpose:** Template for developers to create their own `application-dev.yml`
**Sensitive Data:** L NO - Safe to commit (fake credentials only)

---

## Git Security

### Before First Commit

**CRITICAL:** Check for sensitive data before committing:

```bash
# 1. Check what files are staged
git status

# 2. Review each file before committing
git diff --cached

# 3. Look for these patterns:
#    - Passwords in plain text
#    - API keys and tokens
#    - Database connection strings with real hosts
#    - JWT secrets
```

### If You Accidentally Committed Secrets

**DANGER:** Once pushed to GitHub, secrets are considered **COMPROMISED** even after deletion!

#### Option 1: Not Yet Pushed (Safe)

```bash
# Remove file from staging area
git restore --staged src/main/resources/application-dev.yml

# Or remove from Git history entirely
git rm --cached src/main/resources/application-dev.yml

# Verify .gitignore contains the file
cat .gitignore | grep "application-dev.yml"
```

#### Option 2: Already Pushed (CRITICAL)

1. **Immediately rotate all compromised credentials:**
   - Change Neon PostgreSQL password
   - Generate new JWT_SECRET
   - Revoke API keys (Mercado Pago, Twilio, AWS, etc.)

2. **Use BFG Repo-Cleaner to remove from history:**
   ```bash
   # Install BFG (https://rtyley.github.io/bfg-repo-cleaner/)
   java -jar bfg-1.14.0.jar --delete-files application-dev.yml pitstop-repo.git
   ```

3. **Force push (DANGEROUS - coordinate with team):**
   ```bash
   git push --force
   ```

4. **Update all secrets in production immediately**

---

## JWT Security

### Generating Secure JWT Secrets

**Minimum requirement:** 512 bits (64 bytes) for HS512 algorithm

#### Method 1: Development Endpoint (Easiest)

```bash
# Start the application
./mvnw spring-boot:run

# Visit in browser or curl:
curl http://localhost:8080/api/debug/generate-jwt-secret
```

**Output example:**
```
JWT_SECRET (copy to application-dev.yml):
dGVzdGluZ0pXVFNlY3JldEtleUZvclBpdFN0b3BBcHBsaWNhdGlvbkRldmVsb3BtZW50T25seUNoYW5nZUluUHJvZHVjdGlvbg==
```

#### Method 2: OpenSSL (Linux/Mac/WSL)

```bash
openssl rand -base64 64
```

#### Method 3: PowerShell (Windows)

```powershell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

### JWT Token Lifespan

Configured in `application.yml`:

```yaml
application:
  jwt:
    access-token-expiration: 900000      # 15 minutes
    refresh-token-expiration: 604800000  # 7 days
```

**Production recommendations:**
- Access Token: 15-30 minutes
- Refresh Token: 7-30 days (stored in Redis with rotation)

### Token Revocation

Refresh tokens are stored in Redis and can be revoked via:
- Logout endpoint: `POST /api/auth/logout`
- Admin panel (future feature)
- Redis CLI: `DEL refresh:token:{token}`

---

## Database Security

### Neon PostgreSQL

**Connection Security:**
-  SSL/TLS enabled by default (`sslmode=require`)
-  Channel binding required
-  Automatic connection pooling (PgBouncer)
-  IP allowlist (configure in Neon console)

**Credentials Management:**
- **Development:** Store in `application-dev.yml` (local, NOT committed)
- **Production:** Use environment variables or AWS Secrets Manager

**Password Policy:**
- Minimum 16 characters
- Mix of uppercase, lowercase, numbers, special chars
- Rotate every 90 days in production

### SQL Injection Prevention

**Already implemented:**
-  JPA/Hibernate with parameterized queries
-  Spring Data JPA repositories (safe by default)
-  Input validation with `@Valid` and Bean Validation

**Manual queries:**
```java
//  SAFE - Parameterized query
@Query("SELECT u FROM Usuario u WHERE u.email = :email")
Usuario findByEmail(@Param("email") String email);

// L DANGEROUS - SQL Injection vulnerable
@Query(value = "SELECT * FROM usuarios WHERE email = '" + email + "'", nativeQuery = true)
```

---

## Deployment Security

### Environment Variables (Production)

**NEVER hardcode secrets in production!** Use:

#### Option 1: Environment Variables (Simplest)

```bash
export DATABASE_URL=jdbc:postgresql://...
export DATABASE_USERNAME=your_username
export DATABASE_PASSWORD=your_secure_password
export JWT_SECRET=your_base64_encoded_secret
```

Spring Boot automatically loads these.

#### Option 2: AWS Secrets Manager (Recommended)

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>com.amazonaws.secretsmanager</groupId>
    <artifactId>aws-secretsmanager-jdbc</artifactId>
</dependency>
```

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc-secretsmanager:postgresql://...
    username: secret-name
```

#### Option 3: HashiCorp Vault (Enterprise)

For highly sensitive deployments with rotation policies.

### HTTPS/TLS

**Development:** HTTP is acceptable (localhost)

**Production:** HTTPS is **MANDATORY**

- Use Let's Encrypt for free SSL certificates
- Configure reverse proxy (Nginx/Apache) or load balancer
- Set HSTS headers:
  ```yaml
  server:
    ssl:
      enabled: true
      key-store: classpath:keystore.p12
      key-store-password: ${KEYSTORE_PASSWORD}
  ```

### CORS Configuration

Currently allows:
```yaml
application:
  cors:
    allowed-origins: http://localhost:3000,http://localhost:5173
```

**Production:** Replace with your actual frontend domain:
```yaml
allowed-origins: https://pitstop.com,https://app.pitstop.com
```

### Rate Limiting (Future)

Protect against brute-force attacks:
- `/api/auth/login`: 5 attempts per 15 minutes per IP
- Other endpoints: 100 requests per minute per user

---

## Security Checklist

### Before Every Commit

- [ ] Run `git status` and review staged files
- [ ] Ensure no `application-dev.yml`, `.env`, or `.key` files are staged
- [ ] Search for "password", "secret", "token" in diffs: `git diff --cached | grep -i "password"`
- [ ] Check `.gitignore` is up-to-date

### Before Production Deployment

- [ ] All secrets moved to environment variables or secrets manager
- [ ] JWT_SECRET rotated from development value
- [ ] Database password rotated
- [ ] HTTPS/TLS enabled
- [ ] CORS restricted to production domain only
- [ ] Remove or secure `/api/debug/**` endpoints
- [ ] Enable Redis authentication (`requirepass`)
- [ ] Configure firewall rules (allow only necessary ports)
- [ ] Enable database IP allowlist
- [ ] Set up monitoring and alerting (failed logins, errors)

### Regular Maintenance

- [ ] Rotate JWT_SECRET every 90 days
- [ ] Rotate database passwords every 90 days
- [ ] Review user access logs monthly
- [ ] Update dependencies for security patches
- [ ] Run security scans: `mvn dependency-check:check`

---

## Reporting Security Issues

If you discover a security vulnerability:

1. **DO NOT** open a public GitHub issue
2. Email: security@pitstop.com (if available)
3. Or contact the repository owner directly

Include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

---

## Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [Neon Security Best Practices](https://neon.tech/docs/security)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)

---

**Last Updated:** 2025-10-18
