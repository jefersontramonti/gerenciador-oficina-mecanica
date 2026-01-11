# PitStop - Melhorias de Segurança Pendentes

**Data da Auditoria:** 11 de janeiro de 2026
**Status:** Pendente de implementação

---

## Resumo

| Severidade | Quantidade | Status |
|------------|------------|--------|
| CRITICA | 2 | Pendente |
| ALTA | 3 | Pendente |
| MEDIA | 3 | Pendente |

---

## P1 - IMPLEMENTAR ANTES DE PRODUÇÃO

### 1. [CRÍTICA] Reduzir Expiração do Access Token

**Problema:** Access token expira em 4 horas, muito longo para segurança.

**Arquivo:** `src/main/resources/application.yml`

**Linha 194 - DE:**
```yaml
access-token-expiration: 14400000 # 4 hours
```

**PARA:**
```yaml
access-token-expiration: 900000 # 15 minutes
```

**Frontend - Implementar Silent Refresh:**

Criar arquivo `frontend/src/shared/hooks/useTokenRefresh.ts`:
```typescript
import { useEffect, useRef } from 'react';
import { useAuth } from '@/features/auth/hooks/useAuth';

/**
 * Hook que renova o token automaticamente antes de expirar.
 * Deve ser usado no componente raiz (App.tsx ou AuthInitializer).
 */
export const useTokenRefresh = () => {
  const { isAuthenticated, initialize } = useAuth();
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
      return;
    }

    // Renovar token a cada 14 minutos (1 minuto antes de expirar)
    const REFRESH_INTERVAL = 14 * 60 * 1000; // 14 minutes

    intervalRef.current = setInterval(async () => {
      try {
        console.debug('[TokenRefresh] Renovando token automaticamente...');
        await initialize();
        console.debug('[TokenRefresh] Token renovado com sucesso');
      } catch (error) {
        console.error('[TokenRefresh] Falha ao renovar token:', error);
        // Usuário será redirecionado para login pelo interceptor
      }
    }, REFRESH_INTERVAL);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [isAuthenticated, initialize]);
};
```

**Usar no AuthInitializer:**
```typescript
// frontend/src/features/auth/components/AuthInitializer.tsx
import { useTokenRefresh } from '@/shared/hooks/useTokenRefresh';

export const AuthInitializer = ({ children }: AuthInitializerProps) => {
  // ... código existente ...

  // Adicionar esta linha:
  useTokenRefresh();

  // ... resto do código ...
};
```

---

### 2. [ALTA] Especificar Endpoints Públicos Explicitamente

**Problema:** Padrão `/api/public/**` é muito genérico.

**Arquivo:** `src/main/java/com/pitstop/config/SecurityConfig.java`

**Linha 115-132 - DE:**
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/public/**",           // ← GENÉRICO DEMAIS
            "/api/health",
            "/api/webhooks/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/info",
            "/ws/**"
    ).permitAll()
    .anyRequest().authenticated()
)
```

**PARA:**
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
            // Auth endpoints
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            // Public endpoints (específicos)
            "/api/public/oficinas/register",
            "/api/public/orcamento/**",
            // Health & Webhooks
            "/api/health",
            "/api/webhooks/mercadopago",
            // Documentation (desabilitar em prod via env)
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            // Actuator
            "/actuator/health",
            "/actuator/info",
            // WebSocket
            "/ws/**"
    ).permitAll()
    .anyRequest().authenticated()
)
```

---

### 3. [MÉDIA] Desabilitar Swagger em Produção

**Arquivo:** `src/main/resources/application.yml`

**Linha 183-186 - DE:**
```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

**PARA:**
```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    enabled: ${SWAGGER_ENABLED:false}
  api-docs:
    enabled: ${SWAGGER_ENABLED:false}
```

**Em desenvolvimento, adicionar ao .env:**
```bash
SWAGGER_ENABLED=true
```

---

### 4. [ALTA] Adicionar Rate Limiting no Registro de Oficina

**Arquivo:** `src/main/java/com/pitstop/oficina/controller/PublicOficinaController.java`

**Adicionar import:**
```java
import com.pitstop.shared.security.RateLimitService;
import org.springframework.http.HttpStatus;
```

**Injetar dependência:**
```java
@RestController
@RequestMapping("/api/public/oficinas")
@RequiredArgsConstructor
public class PublicOficinaController {

    private final OficinaService oficinaService;
    private final RateLimitService rateLimitService; // ← ADICIONAR

    @Value("${ratelimit.signup.max-requests:3}")
    private int signupMaxRequests;

    @Value("${ratelimit.signup.window-seconds:86400}")
    private long signupWindowSeconds;
```

**No método register, adicionar verificação:**
```java
@PostMapping("/register")
public ResponseEntity<?> registerOficina(
        @Valid @RequestBody RegisterOficinaRequest request,
        HttpServletRequest httpRequest) {

    // Rate limiting: 3 registros por IP por dia
    String clientIp = getClientIp(httpRequest);
    if (!rateLimitService.isAllowed("signup:" + clientIp, signupMaxRequests, signupWindowSeconds)) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Muitas tentativas. Tente novamente em 24 horas."));
    }

    // ... resto do código existente ...
}

private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
        return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
}
```

**Adicionar ao application.yml:**
```yaml
ratelimit:
  signup:
    max-requests: ${RATELIMIT_SIGNUP_MAX:3}
    window-seconds: ${RATELIMIT_SIGNUP_WINDOW:86400}  # 24 horas
```

---

### 5. [ALTA] Validar X-Forwarded-Proto em Produção

**Arquivo:** `src/main/java/com/pitstop/shared/controller/AuthController.java`

**Método isSecureRequest (linha 551-565) - MELHORAR:**
```java
private boolean isSecureRequest(HttpServletRequest request) {
    // In production, ALWAYS require HTTPS
    if ("prod".equalsIgnoreCase(activeProfile)) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto == null || !"https".equalsIgnoreCase(forwardedProto)) {
            log.warn("SECURITY: Request without X-Forwarded-Proto=https in production. IP: {}",
                     maskIp(request.getRemoteAddr()));
            // Ainda retorna true para não quebrar, mas loga o warning
        }
        return true;
    }

    // Check X-Forwarded-Proto header (set by reverse proxies)
    String forwardedProto = request.getHeader("X-Forwarded-Proto");
    if (forwardedProto != null) {
        return "https".equalsIgnoreCase(forwardedProto);
    }

    return request.isSecure();
}

private String maskIp(String ip) {
    if (ip == null) return "***";
    String[] parts = ip.split("\\.");
    if (parts.length == 4) {
        return parts[0] + "." + parts[1] + ".***." + "***";
    }
    return "***";
}
```

---

## P2 - IMPLEMENTAR ANTES DE ESCALAR (Multi-Instance)

### 6. [CRÍTICA em ambiente distribuído] Migrar Rate Limiting para Redis

**Problema:** Rate limiting atual usa `ConcurrentHashMap` (memória local). Em ambiente com múltiplas instâncias, cada servidor tem seu próprio limite.

**Arquivo:** `src/main/java/com/pitstop/shared/security/RateLimitService.java`

**Substituir implementação atual por:**
```java
package com.pitstop.shared.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Rate limiting service usando Redis para suporte distribuído.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    /**
     * Verifica se a requisição é permitida dentro do limite.
     *
     * @param key identificador único (ex: "login:192.168.1.1")
     * @param maxRequests número máximo de requisições permitidas
     * @param windowSeconds janela de tempo em segundos
     * @return true se permitido, false se limite excedido
     */
    public boolean isAllowed(String key, int maxRequests, long windowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + key;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(redisKey);

            if (currentCount == null) {
                return true; // Fallback: permitir se Redis falhar
            }

            // Se é a primeira requisição, definir TTL
            if (currentCount == 1) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
            }

            boolean allowed = currentCount <= maxRequests;

            if (!allowed) {
                log.warn("Rate limit exceeded for key: {} (count: {}, max: {})",
                         maskIdentifier(key), currentCount, maxRequests);
            }

            return allowed;

        } catch (Exception e) {
            log.error("Redis rate limit error for key {}: {}", maskIdentifier(key), e.getMessage());
            // Fallback: permitir se Redis estiver indisponível
            return true;
        }
    }

    /**
     * Retorna quantas requisições restam para o limite.
     */
    public long getRemainingRequests(String key, int maxRequests) {
        String redisKey = RATE_LIMIT_PREFIX + key;

        try {
            String value = redisTemplate.opsForValue().get(redisKey);
            if (value == null) {
                return maxRequests;
            }
            long current = Long.parseLong(value);
            return Math.max(0, maxRequests - current);
        } catch (Exception e) {
            return maxRequests;
        }
    }

    /**
     * Reseta o contador para uma chave específica.
     */
    public void reset(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        redisTemplate.delete(redisKey);
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 8) {
            return "***";
        }
        return identifier.substring(0, 4) + "***" + identifier.substring(identifier.length() - 4);
    }
}
```

**Remover o scheduler de cleanup** (Redis TTL cuida disso automaticamente).

---

## P3 - MELHORIAS OPCIONAIS

### 7. [MÉDIA] Restringir Cookie Path

**Arquivo:** `src/main/java/com/pitstop/shared/controller/AuthController.java`

**Linha 72 - Considerar mudar de:**
```java
private static final String COOKIE_PATH = "/";
```

**Para (apenas se não usar reverse proxy complexo):**
```java
private static final String COOKIE_PATH = "/api/auth";
```

> **Nota:** Manter "/" se houver reverse proxy que muda paths.

---

### 8. [MÉDIA] Restringir Actuator Prometheus

**Arquivo:** `src/main/resources/application.yml`

**Linha 150-152 - DE:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

**PARA (produção):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

**OU proteger com autenticação:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    prometheus:
      enabled: ${PROMETHEUS_ENABLED:false}
```

---

### 9. [BAIXA] Validação de Formato de Telefone

**Arquivos DTOs:** Adicionar regex para telefone brasileiro.

```java
@Pattern(
    regexp = "^(\\(\\d{2}\\)\\s?)?(9?\\d{4}-?\\d{4}|\\d{10,11})$",
    message = "Telefone deve estar no formato (XX) 9XXXX-XXXX ou XXXXXXXXXXX"
)
private String telefone;
```

---

### 10. [BAIXA] Headers de Segurança Adicionais

**Arquivo:** `src/main/java/com/pitstop/config/SecurityConfig.java`

**Adicionar no método securityFilterChain:**
```java
.headers(headers -> headers
    .frameOptions(frame -> frame.deny())
    .contentTypeOptions(content -> {})
    .xssProtection(xss -> xss.disable())
    .contentSecurityPolicy(csp -> csp
            .policyDirectives("default-src 'self'; frame-ancestors 'none';"))
    // ADICIONAR:
    .permissionsPolicy(permissions -> permissions
            .policy("geolocation=(), microphone=(), camera=()"))
    .referrerPolicy(referrer -> referrer
            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
)
```

---

### 11. [BAIXA] OWASP Dependency Check no CI/CD

**Adicionar ao pom.xml:**
```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.9</version>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
    </configuration>
</plugin>
```

**Rodar manualmente:**
```bash
mvn org.owasp:dependency-check-maven:check
```

---

## Checklist de Implementação

### P1 - Antes de Produção
- [ ] Reduzir access token para 15 minutos
- [ ] Implementar useTokenRefresh hook no frontend
- [ ] Usar hook no AuthInitializer
- [ ] Especificar endpoints públicos explicitamente
- [ ] Adicionar SWAGGER_ENABLED=false como padrão
- [ ] Adicionar rate limiting no registro de oficina
- [ ] Melhorar validação de X-Forwarded-Proto

### P2 - Antes de Escalar
- [ ] Migrar RateLimitService para Redis
- [ ] Remover cleanup scheduler (usar Redis TTL)
- [ ] Testar em ambiente com múltiplas instâncias

### P3 - Melhorias
- [ ] Avaliar mudança de cookie path
- [ ] Restringir/proteger Prometheus endpoint
- [ ] Adicionar validação de telefone
- [ ] Adicionar headers de segurança extras
- [ ] Configurar OWASP Dependency Check

---

## Boas Práticas Já Implementadas

- ✅ JWT HS512 com secret de 64 bytes
- ✅ BCrypt 12 rounds para senhas
- ✅ Refresh token em HttpOnly cookie
- ✅ Access token em memória no frontend
- ✅ @PreAuthorize em todos os controllers
- ✅ Multi-tenancy com TenantContext
- ✅ Queries parametrizadas (Spring Data JPA)
- ✅ Rate limiting em endpoints públicos
- ✅ Mascaramento de dados sensíveis em logs
- ✅ Webhook signature validation (HMAC-SHA256)
- ✅ Headers de segurança básicos

---

**Última atualização:** 11 de janeiro de 2026
