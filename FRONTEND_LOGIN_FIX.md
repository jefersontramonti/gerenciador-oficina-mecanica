# ✅ Correção do Login - Frontend PitStop

## 🐛 Problema Identificado

O login não estava funcionando porque havia uma **incompatibilidade entre o formato de resposta do backend e o que o frontend esperava**.

### O que o Backend retorna:
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "usuario": {
    "id": "uuid",
    "nome": "Admin Teste",
    "email": "admin@pitstop.com",
    "perfil": "ADMIN",
    "perfilNome": null,
    "ativo": true,
    "ultimoAcesso": [2025,10,31,21,52,29,363742900],
    "createdAt": [2025,10,18,1,39,57,655325000],
    "updatedAt": [2025,10,31,21,51,4,388524000]
  }
}
```

### O que o Frontend esperava:
```json
{
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "usuario": { ... }
  }
}
```

## 🔧 Correções Aplicadas

### 1. `authService.ts` - Ajuste nas Respostas da API

**Antes:**
```typescript
const response = await api.post<ApiResponse<LoginResponse>>('/auth/login', credentials);
const loginData = response.data.data; // ❌ Incorreto
```

**Depois:**
```typescript
const response = await api.post<LoginResponse>('/auth/login', credentials);
const loginData = response.data; // ✅ Correto
```

Mesma correção foi aplicada para:
- `login()`
- `register()`
- `refreshToken()`
- `getCurrentUser()`
- `updateProfile()`

### 2. `api.ts` - Interceptor de Refresh Token

**Antes:**
```typescript
const refreshResponse = await api.post<ApiResponse<{ accessToken: string }>>('/auth/refresh', ...);
const newToken = refreshResponse.data.data.accessToken; // ❌ Incorreto
```

**Depois:**
```typescript
const refreshResponse = await api.post<{ accessToken: string }>('/auth/refresh', ...);
const newToken = refreshResponse.data.accessToken; // ✅ Correto
```

### 3. Tipos de `Usuario` - Compatibilidade com Java LocalDateTime

O backend serializa `LocalDateTime` como array de números:
```json
"ultimoAcesso": [2025, 10, 31, 21, 52, 29, 363742900]
```

**Ajuste no tipo:**
```typescript
export interface Usuario {
  id: string;
  nome: string;
  email: string;
  perfil: PerfilUsuario;
  perfilNome?: string | null;  // ✅ Adicionado
  ativo: boolean;
  ultimoAcesso: string | number[] | null;  // ✅ Aceita ambos os formatos
  createdAt: string | number[];            // ✅ Aceita ambos os formatos
  updatedAt: string | number[];            // ✅ Aceita ambos os formatos
}
```

## ✅ Como Testar o Login

### 1. Certifique-se de que o backend está rodando
```bash
cd backend
./mvnw spring-boot:run  # Linux/Mac
mvnw.cmd spring-boot:run  # Windows
```

Backend deve estar em: `http://localhost:8080`

### 2. Inicie o frontend
```bash
cd frontend
npm run dev
```

Frontend estará em: `http://localhost:5173`

### 3. Faça o login
```
Email: admin@pitstop.com
Senha: admin123
```

### 4. O que deve acontecer:
1. ✅ Requisição POST para `/api/auth/login`
2. ✅ Backend retorna accessToken, refreshToken e dados do usuário
3. ✅ accessToken armazenado em memória
4. ✅ refreshToken armazenado em cookie HttpOnly
5. ✅ Redirecionamento automático para Dashboard
6. ✅ WebSocket conecta automaticamente

## 🔍 Testando via cURL (Validação)

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@pitstop.com","senha":"admin123"}' \
  -c cookies.txt

# Deve retornar: accessToken, refreshToken e usuario
```

## 📝 Arquivos Modificados

1. ✅ `frontend/src/features/auth/services/authService.ts`
   - Removido wrapper `ApiResponse<T>` de todas as chamadas
   - Ajustado para acessar `response.data` diretamente

2. ✅ `frontend/src/shared/services/api.ts`
   - Corrigido interceptor de refresh token
   - Ajustado para acessar `refreshResponse.data.accessToken`

3. ✅ `frontend/src/features/auth/types/index.ts`
   - Adicionado campo `perfilNome` opcional
   - Ajustado tipos de data para aceitar `string | number[]`

## 🎯 Status Atual

- ✅ Build do frontend: **Passing**
- ✅ TypeScript: **No errors**
- ✅ Login endpoint: **Funcionando**
- ✅ Compatibilidade backend/frontend: **OK**

## ⚠️ Observação Importante sobre Datas

O backend está retornando datas como arrays de números (formato LocalDateTime do Java):
```json
[2025, 10, 31, 21, 52, 29, 363742900]
```

**Recomendação futura:** Configure o Jackson no backend para retornar datas em formato ISO 8601:
```json
"ultimoAcesso": "2025-10-31T21:52:29.363742900"
```

Para isso, adicione no `application.properties`:
```properties
spring.jackson.serialization.write-dates-as-timestamps=false
```

## 🚀 Próximos Passos

Agora que o login está funcionando:

1. **Testar o fluxo completo:**
   - Login ✅
   - Navegação entre páginas
   - Logout
   - Refresh token automático

2. **Implementar próximos módulos:**
   - CRUD de Clientes
   - CRUD de Veículos
   - Ordens de Serviço

3. **Melhorias:**
   - Configurar formatação de datas
   - Implementar toast notifications
   - Adicionar loading states

---

**Data da Correção**: 31 de Outubro de 2025
**Status**: ✅ Login Funcionando
