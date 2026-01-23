# PitStop - Checklist de Testes Unitarios

**Ultima atualizacao:** 2026-01-22
**Status Geral:** Em andamento

---

## Resumo

| Modulo | Services | Controllers | Repositories | Status |
|--------|----------|-------------|--------------|--------|
| Cliente | [x] 1/1 | [ ] 0/1 | [ ] 0/1 | Parcial |
| Veiculo | [x] 1/1 | [ ] 0/1 | [ ] 0/1 | Parcial |
| Ordem de Servico | [x] 1/2 | [ ] 0/1 | [ ] 0/1 | Parcial |
| Estoque | [x] 3/3 | [ ] 0/3 | [ ] 0/3 | Parcial |
| Financeiro | [ ] 0/9 | [ ] 0/5 | [ ] 0/5 | Pendente |
| Usuario | [ ] 0/1 | [ ] 0/1 | [ ] 0/1 | Pendente |
| Oficina | [x] 1/2 | [ ] 0/1 | [ ] 0/1 | Parcial |
| Notificacao | [ ] 0/8 | [ ] 0/2 | [ ] 0/2 | Pendente |
| Manutencao Preventiva | [ ] 0/6 | [ ] 0/4 | [ ] 0/4 | Pendente |
| SaaS | [ ] 0/14 | [ ] 0/8 | [ ] 0/8 | Pendente |
| Dashboard | [ ] 0/1 | [ ] 0/1 | [ ] 0/0 | Pendente |
| Webhook | [ ] 0/1 | [ ] 0/1 | [ ] 0/1 | Pendente |
| Anexo | [ ] 0/2 | [ ] 0/1 | [ ] 0/1 | Pendente |
| IA | [ ] 0/5 | [ ] 0/1 | [ ] 0/1 | Pendente |
| Shared (Auth) | [ ] 0/2 | [ ] 0/1 | [ ] 0/1 | Pendente |

---

## Detalhamento por Modulo

### 1. CLIENTE

**Services:**
- [x] `ClienteService` (21 testes - ClienteServiceTest.java)
  - [x] criar() - sucesso (PF e PJ)
  - [x] criar() - CPF/CNPJ duplicado
  - [x] criar() - validacao PF com CNPJ
  - [x] criar() - validacao PJ com CPF
  - [x] criar() - CPF invalido (digitos verificadores)
  - [x] atualizar() - sucesso
  - [x] atualizar() - nao encontrado
  - [x] buscarPorId() - sucesso
  - [x] buscarPorId() - nao encontrado
  - [x] buscarPorCpfCnpj() - sucesso
  - [x] buscarPorCpfCnpj() - nao encontrado
  - [x] listar() - paginacao
  - [x] listar() - pagina vazia
  - [x] buscarPorTipo() - filtro por tipo
  - [x] buscarPorNome() - busca parcial
  - [x] deletar() - soft delete sucesso
  - [x] deletar() - nao encontrado
  - [x] reativar() - sucesso
  - [x] reativar() - nao encontrado
  - [x] countByTipo() - contagem

**Controllers:**
- [ ] `ClienteController`
  - [ ] POST /api/clientes - 201 Created
  - [ ] POST /api/clientes - 400 Bad Request
  - [ ] POST /api/clientes - 409 Conflict (duplicado)
  - [ ] GET /api/clientes - 200 OK (lista)
  - [ ] GET /api/clientes/{id} - 200 OK
  - [ ] GET /api/clientes/{id} - 404 Not Found
  - [ ] PUT /api/clientes/{id} - 200 OK
  - [ ] DELETE /api/clientes/{id} - 204 No Content

**Repositories:**
- [ ] `ClienteRepository`
  - [ ] findByOficinaId()
  - [ ] findByCpfCnpjAndOficinaId()
  - [ ] existsByCpfCnpjAndOficinaId()

---

### 2. VEICULO

**Services:**
- [x] `VeiculoService` (23 testes - VeiculoServiceTest.java)
  - [x] criar() - sucesso
  - [x] criar() - placa duplicada
  - [x] criar() - cliente inexistente
  - [x] criar() - normaliza placa com hifen
  - [x] buscarPorId() - sucesso
  - [x] buscarPorId() - nao encontrado
  - [x] buscarPorPlaca() - sucesso
  - [x] buscarPorPlaca() - normaliza placa
  - [x] buscarPorPlaca() - nao encontrado
  - [x] listar() - paginacao
  - [x] listar() - pagina vazia
  - [x] buscarPorClienteId() - sucesso
  - [x] buscarPorClienteId() - cliente inexistente
  - [x] atualizar() - sucesso
  - [x] atualizar() - nao encontrado
  - [x] atualizarQuilometragem() - sucesso
  - [x] atualizarQuilometragem() - nao encontrado
  - [x] deletar() - sucesso
  - [x] deletar() - nao encontrado
  - [x] findMarcas() - lista unica
  - [x] findModelos() - lista unica
  - [x] findAnos() - lista unica
  - [x] countByClienteId() - contagem

**Controllers:**
- [ ] `VeiculoController`
  - [ ] POST /api/veiculos - 201 Created
  - [ ] POST /api/veiculos - 409 Conflict (placa duplicada)
  - [ ] GET /api/veiculos - 200 OK
  - [ ] GET /api/veiculos/{id} - 200 OK
  - [ ] GET /api/veiculos/placa/{placa} - 200 OK
  - [ ] PUT /api/veiculos/{id} - 200 OK
  - [ ] DELETE /api/veiculos/{id} - 204 No Content

**Repositories:**
- [ ] `VeiculoRepository`
  - [ ] findByPlacaAndOficinaId()
  - [ ] findByClienteId()
  - [ ] existsByPlacaAndOficinaId()

---

### 3. ORDEM DE SERVICO

**Services:**
- [x] `OrdemServicoService` (19 testes - OrdemServicoServiceTest.java)
  - [x] criar() - sucesso
  - [x] criar() - veiculo inexistente
  - [x] criar() - mecanico inexistente
  - [x] buscarPorId() - sucesso
  - [x] buscarPorId() - nao encontrado
  - [x] buscarPorNumero() - sucesso
  - [x] buscarPorNumero() - nao encontrado
  - [x] aprovar() - sucesso
  - [x] aprovar() - status invalido
  - [x] iniciar() - sucesso
  - [x] iniciar() - status invalido
  - [x] finalizar() - sucesso
  - [x] finalizar() - status invalido
  - [x] entregar() - sucesso (quitada)
  - [x] entregar() - nao paga
  - [x] cancelar() - sucesso
  - [x] cancelar() - ja entregue
  - [x] atualizar() - status editavel
  - [x] atualizar() - status nao editavel

- [ ] `OrdemServicoPDFService`
  - [ ] gerarPDF() - sucesso
  - [ ] gerarPDF() - com itens
  - [ ] gerarPDF() - sem itens

**Controllers:**
- [ ] `OrdemServicoController`
  - [ ] POST /api/ordens-servico - 201 Created
  - [ ] GET /api/ordens-servico - 200 OK (lista com filtros)
  - [ ] GET /api/ordens-servico/{id} - 200 OK
  - [ ] PUT /api/ordens-servico/{id} - 200 OK
  - [ ] PATCH /api/ordens-servico/{id}/status - 200 OK
  - [ ] GET /api/ordens-servico/{id}/pdf - 200 OK

**Repositories:**
- [ ] `OrdemServicoRepository`
  - [ ] findByFilters()
  - [ ] findByNumeroAndOficinaId()
  - [ ] getNextNumero()

---

### 4. ESTOQUE

**Services:**
- [x] `EstoqueService` (28 testes - EstoqueServiceTest.java)
  - [x] criar() - sucesso
  - [x] criar() - quantidade inicial zero quando nao informada
  - [x] criar() - codigo duplicado
  - [x] atualizar() - sucesso
  - [x] atualizar() - nao encontrado
  - [x] atualizar() - codigo duplicado
  - [x] buscarPorId() - sucesso
  - [x] buscarPorId() - nao encontrado
  - [x] buscarPorCodigo() - sucesso
  - [x] buscarPorCodigo() - nao encontrado
  - [x] listarTodas() - paginacao
  - [x] listarTodas() - pagina vazia
  - [x] listarEstoqueBaixo()
  - [x] listarEstoqueZerado()
  - [x] contarEstoqueBaixo()
  - [x] contarEstoqueZerado()
  - [x] desativar() - sucesso
  - [x] desativar() - nao encontrado
  - [x] reativar() - sucesso
  - [x] reativar() - codigo duplicado
  - [x] buscarPorMarca()
  - [x] buscarPorDescricao()
  - [x] calcularValorTotalInventario()
  - [x] listarMarcas()
  - [x] listarPecasSemLocalizacao()
  - [x] contarPecasSemLocalizacao()
  - [x] definirLocalizacaoPeca() - definir
  - [x] definirLocalizacaoPeca() - remover

- [x] `MovimentacaoEstoqueService` (17 testes - MovimentacaoEstoqueServiceTest.java)
  - [x] registrarEntrada() - sucesso
  - [x] registrarEntrada() - peca nao encontrada
  - [x] registrarSaida() - sucesso
  - [x] registrarSaida() - estoque insuficiente
  - [x] registrarSaida() - peca nao encontrada
  - [x] registrarAjuste() - positivo
  - [x] registrarAjuste() - negativo
  - [x] registrarAjuste() - quantidade igual (invalido)
  - [x] baixarEstoquePorOS() - sucesso
  - [x] baixarEstoquePorOS() - sem pecas estoque
  - [x] baixarEstoquePorOS() - ignora pecas avulsas/cliente
  - [x] baixarEstoquePorOS() - estoque insuficiente
  - [x] estornarEstoquePorOS() - sucesso
  - [x] estornarEstoquePorOS() - sem baixas
  - [x] buscarHistoricoPeca()
  - [x] buscarPorOS()
  - [x] buscarComFiltros()

- [x] `LocalArmazenamentoService` (21 testes - LocalArmazenamentoServiceTest.java)
  - [x] criar() - local raiz
  - [x] criar() - local filho
  - [x] criar() - codigo duplicado
  - [x] criar() - tipo requer pai
  - [x] buscarPorId() - sucesso
  - [x] buscarPorId() - nao encontrado
  - [x] buscarPorCodigo() - sucesso
  - [x] buscarPorCodigo() - nao encontrado
  - [x] listarTodos()
  - [x] listarLocaisRaiz()
  - [x] listarFilhos()
  - [x] listarPorTipo()
  - [x] buscarPorDescricao()
  - [x] atualizar() - sucesso
  - [x] atualizar() - codigo duplicado
  - [x] atualizar() - ciclo hierarquico
  - [x] desativar() - sucesso
  - [x] reativar() - sucesso
  - [x] excluir() - sucesso
  - [x] excluir() - com pecas vinculadas
  - [x] excluir() - nao encontrado

**Controllers:**
- [ ] `PecaController`
- [ ] `MovimentacaoController`
- [ ] `LocalArmazenamentoController`

**Repositories:**
- [ ] `PecaRepository`
- [ ] `MovimentacaoEstoqueRepository`
- [ ] `LocalArmazenamentoRepository`

---

### 5. FINANCEIRO

**Services:**
- [ ] `PagamentoService`
  - [ ] criar()
  - [ ] confirmar()
  - [ ] cancelar()
  - [ ] estornar()
  - [ ] listar()

- [ ] `NotaFiscalService`
  - [ ] criar()
  - [ ] emitir()
  - [ ] cancelar()
  - [ ] gerarPDF()

- [ ] `DespesaService`
  - [ ] criar()
  - [ ] atualizar()
  - [ ] pagar()
  - [ ] cancelar()

- [ ] `FluxoCaixaService`
  - [ ] calcularFluxo()
  - [ ] obterResumo()

- [ ] `AssinaturaService`
- [ ] `ParcelamentoService`
- [ ] `ConciliacaoService`
- [ ] `MercadoPagoService`

**Controllers:**
- [ ] `PagamentoController`
- [ ] `NotaFiscalController`
- [ ] `DespesaController`
- [ ] `FluxoCaixaController`
- [ ] `AssinaturaController`

---

### 6. USUARIO

**Services:**
- [ ] `UsuarioService`
  - [ ] criar() - sucesso
  - [ ] criar() - email duplicado
  - [ ] atualizar()
  - [ ] buscarPorId()
  - [ ] buscarPorEmail()
  - [ ] alterarSenha()
  - [ ] resetarSenha()
  - [ ] ativar()
  - [ ] desativar()

**Controllers:**
- [ ] `UsuarioController`
  - [ ] POST /api/usuarios
  - [ ] GET /api/usuarios
  - [ ] GET /api/usuarios/{id}
  - [ ] PUT /api/usuarios/{id}
  - [ ] DELETE /api/usuarios/{id}

---

### 7. OFICINA

**Services:**
- [x] `OficinaService` (existe: OficinaServiceTest.java)
  - [x] Testes basicos implementados

- [ ] `OficinaRegistrationService`
  - [ ] registrar()
  - [ ] validarCnpj()

**Controllers:**
- [ ] `OficinaController`

---

### 8. NOTIFICACAO

**Services:**
- [ ] `NotificacaoOrchestrator`
  - [ ] notificarEventoOS()
  - [ ] enviarPorCanal()
  - [ ] selecionarCanaisPrioritarios()

- [ ] `WhatsAppService`
  - [ ] enviarMensagem()
  - [ ] verificarConexao()

- [ ] `EmailService`
  - [ ] enviar()
  - [ ] enviarComAnexo()

- [ ] `TelegramService`
  - [ ] enviarMensagem()

- [ ] `ConfiguracaoNotificacaoService`
- [ ] `HistoricoNotificacaoService`
- [ ] `TemplateService`
- [ ] `WebSocketNotificationService`

---

### 9. MANUTENCAO PREVENTIVA

**Services:**
- [ ] `PlanoManutencaoService`
  - [ ] criar()
  - [ ] atualizar()
  - [ ] executar()
  - [ ] pausar()
  - [ ] retomar()
  - [ ] verificarVencimento()

- [ ] `AgendamentoManutencaoService`
  - [ ] criar()
  - [ ] confirmar()
  - [ ] remarcar()
  - [ ] cancelar()
  - [ ] enviarNotificacoes()

- [ ] `AgendamentoPublicService`
  - [ ] buscarPorToken()
  - [ ] confirmar()
  - [ ] rejeitar()

- [ ] `AlertaManutencaoService`
  - [ ] processarAlertasPendentes()
  - [ ] enviarAlerta()

- [ ] `TemplateManutencaoService`
- [ ] `DashboardManutencaoService`

**Controllers:**
- [ ] `PlanoManutencaoController`
- [ ] `AgendamentoManutencaoController`
- [ ] `AgendamentoPublicController`
- [ ] `TemplateManutencaoController`

---

### 10. SAAS (SUPER_ADMIN)

**Services:**
- [ ] `SaasOficinaService`
- [ ] `SaasDashboardService`
- [ ] `FaturaService`
- [ ] `PlanoService`
- [ ] `InadimplenciaService`
- [ ] `ComunicadoService`
- [ ] `TicketService`
- [ ] `FeatureFlagService`
- [ ] `ImpersonationService`
- [ ] `RelatorioService`
- [ ] `SaasAuditService`
- [ ] `SaasPagamentoService`
- [ ] `MinhaContaService`
- [ ] `ConfiguracaoGatewayService`

---

### 11. DASHBOARD

**Services:**
- [ ] `DashboardService`
  - [ ] obterEstatisticas()
  - [ ] obterGraficoFaturamento()
  - [ ] obterOrdensRecentes()

---

### 12. WEBHOOK

**Services:**
- [ ] `WebhookService`
  - [ ] criar()
  - [ ] disparar()
  - [ ] registrarLog()
  - [ ] reenviar()

---

### 13. ANEXO

**Services:**
- [ ] `AnexoService`
  - [ ] upload()
  - [ ] download()
  - [ ] deletar()
  - [ ] listarPorOS()

- [ ] `FileStorageService`
  - [ ] salvar()
  - [ ] carregar()
  - [ ] deletar()

---

### 14. IA (Diagnostico)

**Services:**
- [ ] `DiagnosticoIAService`
- [ ] `ConfiguracaoIAService`
- [ ] `AnthropicClientService`
- [ ] `PreValidacaoService`
- [ ] `ContextCompressionService`

---

### 15. SHARED (Auth/Security)

**Services:**
- [ ] `RefreshTokenService`
  - [ ] criar()
  - [ ] validar()
  - [ ] revogar()
  - [ ] limparExpirados()

- [ ] `PasswordResetService`
  - [ ] solicitar()
  - [ ] validarToken()
  - [ ] resetar()

**Controllers:**
- [ ] `AuthController`
  - [ ] POST /api/auth/login
  - [ ] POST /api/auth/refresh
  - [ ] POST /api/auth/logout
  - [ ] POST /api/auth/forgot-password
  - [ ] POST /api/auth/reset-password
  - [ ] GET /api/auth/me

---

## Testes Existentes

1. **OficinaServiceTest.java** - Testes basicos do servico de oficina (9 testes)
2. **ClienteServiceTest.java** - Testes do servico de cliente (21 testes)
3. **VeiculoServiceTest.java** - Testes do servico de veiculo (23 testes)
4. **OrdemServicoServiceTest.java** - Testes do servico de ordem de servico (19 testes)
5. **EstoqueServiceTest.java** - Testes do servico de pecas (28 testes)
6. **MovimentacaoEstoqueServiceTest.java** - Testes do servico de movimentacao (17 testes)
7. **LocalArmazenamentoServiceTest.java** - Testes do servico de locais (21 testes)
8. **ClienteControllerIntegrationTest.java** - Testes de integracao do controller
9. **HealthCheckControllerIntegrationTest.java** - Teste do endpoint de health

**Total de testes unitarios:** 138 testes

---

## Proximos Passos

1. [x] Implementar testes do modulo **Cliente** (Service)
2. [x] Implementar testes do modulo **Veiculo** (Service)
3. [x] Implementar testes do modulo **Ordem de Servico** (Service)
4. [x] Implementar testes do modulo **Estoque** (Service)
5. [ ] Implementar testes do modulo **Financeiro** (Service)
6. [ ] Implementar testes do modulo **Usuario** (Service)
7. [ ] Implementar testes do modulo **Auth/Security**
8. [ ] Implementar testes de Controllers

---

## Configuracao de Testes

### Dependencias (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

### Comandos
```bash
# Rodar todos os testes
./mvnw test

# Rodar testes de um modulo especifico
./mvnw test -Dtest=ClienteServiceTest

# Rodar com cobertura
./mvnw test jacoco:report
```

---

## Metricas de Cobertura (Meta)

| Tipo | Meta | Atual |
|------|------|-------|
| Line Coverage | 80% | 0% |
| Branch Coverage | 70% | 0% |
| Method Coverage | 85% | 0% |
