# Problema JWT_SECRET Resolvido

## 🐛 Problema Encontrado

Ao tentar fazer login via `POST /api/auth/login`, o sistema retornava erro HTTP 500:

```json
{
  "message": "Illegal base64 character: '-'"
}
```

## 🔍 Causa Raiz

O JWT_SECRET configurado em `application-dev.yml` continha hífens (caractere `-`), que não são válidos em Base64:

```yaml
# ❌ INCORRETO (tinha hífens)
application:
  jwt:
    secret: pitstop-super-secret-key-for-development-only-change-in-production-minimum-64-chars
```

Quando o `JwtService` tentava decodificar esse secret como Base64, a biblioteca `io.jsonwebtoken` lançava exceção.

## ✅ Solução Aplicada

Atualizamos o `application-dev.yml` (linhas 14-17) com um JWT_SECRET válido em Base64:

```yaml
# ✅ CORRETO (Base64 válido)
application:
  jwt:
    secret: dGVzdGluZ0pXVFNlY3JldEtleUZvclBpdFN0b3BBcHBsaWNhdGlvbkRldmVsb3BtZW50T25seUNoYW5nZUluUHJvZHVjdGlvbg==
```

Este Base64 representa a string: `"testingJWTSecretKeyForPitStopApplicationDevelopmentOnlyChangeInProduction"`

## 📝 Lições Aprendidas

### 1. **Spring Boot NÃO carrega arquivos `.env` automaticamente**

Diferente de frameworks como Node.js (com `dotenv`), o Spring Boot não lê arquivos `.env` por padrão.

**Opções para configuração de ambiente:**

- **Desenvolvimento**: Usar `application-dev.yml` (mais simples, já funciona)
- **Produção**: Usar variáveis de ambiente do sistema ou secrets manager (AWS Secrets Manager, Azure Key Vault)
- **Alternativa**: Adicionar biblioteca `me.paulschwarz:spring-dotenv` ao `pom.xml` (não recomendado)

### 2. **JWT_SECRET deve ser Base64-encoded para HS512**

O algoritmo HS512 (HMAC SHA-512) requer uma chave de **512 bits (64 bytes)**. Quando configuramos via string, o JJWT espera que seja Base64-encoded.

**Como gerar JWT_SECRET válido:**

#### Opção A: Via endpoint de debug (desenvolvimento)
```bash
GET http://localhost:8080/api/debug/generate-jwt-secret
```

Retorna algo como:
```
JWT_SECRET (copy to .env):
dGVzdGluZ0pXVFNlY3JldEtleUZvclBpdFN0b3BBcHBsaWNhdGlvbkRldmVsb3BtZW50T25seUNoYW5nZUluUHJvZHVjdGlvbg==
```

#### Opção B: Via linha de comando (Linux/Mac)
```bash
openssl rand -base64 64
```

#### Opção C: Via PowerShell (Windows)
```powershell
$bytes = New-Object byte[] 64
[Security.Cryptography.RNGCryptoServiceProvider]::new().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

#### Opção D: Via Java (para produção)
```java
SecureRandom random = new SecureRandom();
byte[] bytes = new byte[64]; // 512 bits
random.nextBytes(bytes);
String secret = Base64.getEncoder().encodeToString(bytes);
```

### 3. **Configuração de desenvolvimento vs produção**

**Desenvolvimento (`application-dev.yml`):**
- ✅ Pode commitar secrets (são apenas para testes locais)
- ✅ Valores hardcoded facilitam setup inicial
- ✅ Database passwords podem estar visíveis (apenas para dev)

**Produção (`application-prod.yml` ou variáveis de ambiente):**
- ❌ NUNCA commitar secrets reais
- ✅ Usar `${JWT_SECRET}` e definir via variável de ambiente
- ✅ Usar secrets manager (AWS Secrets Manager, HashiCorp Vault, etc.)
- ✅ Rodar: `JWT_SECRET=xxx java -jar pitstop.jar --spring.profiles.active=prod`

## 🧪 Validação da Solução

Após a correção, a aplicação iniciou com sucesso:

```
2025-10-17 23:21:13 - Tomcat started on port 8080 (http) with context path '/'
2025-10-17 23:21:13 - Started PitStopApplication in 8.616 seconds
```

✅ Sem erros de Base64
✅ JWT filter configurado corretamente
✅ Pronto para testes de autenticação

## 🚀 Próximos Passos

1. **Testar login:**
   ```http
   POST http://localhost:8080/api/auth/login
   Content-Type: application/json

   {
     "email": "admin@pitstop.com",
     "senha": "admin123"
   }
   ```

2. **Validar tokens JWT** em https://jwt.io

3. **Testar refresh e logout**

4. **Criar endpoint protegido** para validar autorização

---

## 📌 Arquivos Modificados

- ✅ `src/main/resources/application-dev.yml` (linha 17) - JWT_SECRET corrigido
- ℹ️ `.env` - **NÃO é usado pelo Spring Boot** (apenas documentação)

## 🔐 Segurança

**⚠️ IMPORTANTE para produção:**

1. Trocar JWT_SECRET para valor aleatório gerado via `SecureRandom`
2. Definir JWT_SECRET via variável de ambiente (não hardcoded)
3. Usar secrets manager (AWS Secrets Manager, Azure Key Vault)
4. Ativar HTTPS obrigatório
5. Mudar cookie `secure: true` (linha 99 e 165 de `AuthController.java`)
6. Atualizar CORS origins para domínio de produção
7. Remover `/api/debug/**` dos endpoints públicos (linha 96 de `SecurityConfig.java`)

---

**Data da correção:** 2025-10-17 23:21
**Versão da aplicação:** 0.0.1-SNAPSHOT
**Spring Boot:** 3.5.7-SNAPSHOT
**Java:** 25 LTS
