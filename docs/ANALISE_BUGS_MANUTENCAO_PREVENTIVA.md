# Análise de Bugs - Manutenção Preventiva e Agendamentos

**Data da análise:** 2026-01-26
**Gravidade:** CRÍTICA - Sistema enviando notificações repetidas

---

## Resumo do Problema

O sistema de manutenção preventiva está enviando notificações automáticas repetidamente, mesmo quando:
- Não há agendamentos pendentes
- O agendamento já foi confirmado
- A notificação já foi enviada anteriormente

---

## Arquivos Envolvidos

| Arquivo | Função |
|---------|--------|
| `ManutencaoPreventivaScheduler.java` | Jobs agendados (cron) |
| `AlertaManutencaoService.java` | Processamento e envio de alertas |
| `AlertaManutencaoRepository.java` | Queries de alertas |
| `AgendamentoManutencaoRepository.java` | Queries de agendamentos |
| `PlanoManutencaoPreventiva.java` | Entidade do plano |
| `AgendamentoNotificacao.java` | Agendamento de notificação (JSON) |

---

## BUGS IDENTIFICADOS

### 🔴 BUG 1: Condição de janela de tempo INVERTIDA (CRÍTICO)

**Arquivo:** `ManutencaoPreventivaScheduler.java`
**Linha:** 245-255
**Método:** `processarNotificacoesAgendadas()`

**Código atual (ERRADO):**
```java
// Verifica se está na janela de envio
if (!dataHoraAgendada.isBefore(inicio) || dataHoraAgendada.isAfter(fim)) {
    // Ainda não está na hora ou já passou muito tempo
    if (dataHoraAgendada.isAfter(fim)) {
        continue; // Ainda não chegou a hora
    }
    if (dataHoraAgendada.isBefore(inicio.minusMinutes(30))) {
        // Passou mais de 30 min, marca como erro
        agendamento.marcarComoFalha("Horário de envio perdido");
        continue;
    }
}
```

**Problema:** A lógica está invertida:
- `!dataHoraAgendada.isBefore(inicio)` = TRUE quando está DEPOIS do início
- `dataHoraAgendada.isAfter(fim)` = TRUE quando está DEPOIS do fim
- A condição faz o if entrar quando está NA janela correta, não fora dela!

**Correção:**
```java
// Pula se está FORA da janela de envio
if (dataHoraAgendada.isBefore(inicio) || dataHoraAgendada.isAfter(fim)) {
    if (dataHoraAgendada.isAfter(fim)) {
        continue; // Ainda não chegou a hora
    }
    // Passou mais de 30 min da janela
    if (dataHoraAgendada.isBefore(inicio.minusMinutes(30))) {
        agendamento.marcarComoFalha("Horário de envio perdido");
    }
    continue;
}
```

---

### 🔴 BUG 2: Notificações DUPLICADAS (CRÍTICO)

**Arquivos:** `ManutencaoPreventivaScheduler.java`
**Linhas:** 58-98, 196-206, 323-370

**Problema:** Múltiplos jobs gerando notificações para o mesmo plano:

| Job | Cron | O que faz |
|-----|------|-----------|
| `verificarManutencoesPendentes()` | 8h diário | Cria OS automaticamente |
| `verificarManutencaoVencidas()` | 8h30 diário | Cria alertas |
| `processarEEnviarAlertas()` | A cada 30 min | Processa alertas |
| `processarNotificacoesAgendadas()` | A cada 5 min | Processa notificações |

**Fluxo que causa duplicação:**

1. `verificarManutencoesPendentes()` chama `gerarAlertaParaPlano()`
2. `gerarAlertaParaPlano()` chama `criarOrdemServicoParaManutencao()`
3. A OS criada dispara `OrdemServicoEventListener` → envia email/WhatsApp via NotificacaoOrchestrator
4. MAS também atualiza `ultimoAlertaEnviadoEm` do plano
5. Se o plano tiver `agendamentosNotificacao`, o job `processarNotificacoesAgendadas()` ainda tenta enviar

**Correção:**
- Depois de criar OS automaticamente, marcar todos os `agendamentosNotificacao` como enviados
- OU remover a criação automática de OS e usar apenas alertas

---

### 🔴 BUG 3: Alertas de vencidos reenviados semanalmente

**Arquivo:** `ManutencaoPreventivaScheduler.java`
**Linha:** 116-118
**Método:** `verificarManutencaoVencidas()`

**Código atual:**
```java
.filter(p -> p.getUltimoAlertaEnviadoEm() == null ||
            p.getUltimoAlertaEnviadoEm().isBefore(seteDiasAtras))
```

**Problema:** Planos vencidos recebem alertas **toda semana** indefinidamente enquanto não forem resolvidos.

**Correção:**
- Adicionar campo `alertaVencidoEnviado` boolean no plano
- OU limitar número máximo de alertas de vencimento (ex: 3)

---

### 🟡 BUG 4: Lembrete de agendamento sem verificação de duplicata

**Arquivo:** `ManutencaoPreventivaScheduler.java`
**Linha:** 438-469
**Método:** `enviarLembretesAgendamentos()` + `gerarAlertaLembrete()`

**Problema:**
```java
// gerarAlertaLembrete() cria alerta SEM verificar se já existe
AlertaManutencao alerta = AlertaManutencao.builder()
    .oficina(agendamento.getOficina())
    .plano(agendamento.getPlano())
    // ...
    .build();
alertaRepository.save(alerta);
```

Se o job rodar mais de uma vez no dia, cria alertas duplicados.

**Correção:**
```java
// Verificar antes de criar
if (alertaRepository.existsAlertaPendente(agendamento.getPlano().getId(), TipoAlerta.LEMBRETE_AGENDAMENTO)) {
    return; // Já existe
}
```

---

### 🟡 BUG 5: Falta de transação adequada na atualização de agendamentos

**Arquivo:** `ManutencaoPreventivaScheduler.java`
**Linha:** 271-272

**Código:**
```java
// Salva as atualizações no plano
planoRepository.save(plano);
```

**Problema:** O `agendamento.marcarComoEnviado()` modifica o objeto em memória (JSON), mas se ocorrer erro antes do `save()`, a marcação é perdida e a notificação será reenviada.

---

### 🟡 BUG 6: Criação de OS sem verificar se já existe

**Arquivo:** `ManutencaoPreventivaScheduler.java`
**Linha:** 344-356
**Método:** `gerarAlertaParaPlano()`

**Problema:** Cria OS automaticamente sem verificar se já existe uma OS aberta para o mesmo plano/veículo.

**Consequência:** Se o job rodar e falhar parcialmente, pode criar múltiplas OS para a mesma manutenção.

---

## Jobs Agendados (Cron)

| Job | Cron Padrão | Frequência | Problema |
|-----|-------------|------------|----------|
| `verificarManutencoesPendentes` | `0 0 8 * * ?` | 1x/dia 8h | Cria OS |
| `verificarManutencaoVencidas` | `0 30 8 * * ?` | 1x/dia 8h30 | Cria alertas |
| `enviarLembretesAgendamentos` | `0 0 7 * * ?` | 1x/dia 7h | Cria alertas |
| `atualizarStatusPlanos` | `0 0 1 * * ?` | 1x/dia 1h | OK |
| `processarEEnviarAlertas` | `0 */30 * * * ?` | A cada 30 min | Processa alertas |
| `processarNotificacoesAgendadas` | `0 */5 * * * ?` | A cada 5 min | **BUG 1** |

---

## Fluxo Atual vs Fluxo Esperado

### Fluxo ATUAL (Problemático):

```
┌─────────────────────────────────────────────────────────────────┐
│ 8h: verificarManutencoesPendentes()                             │
│   └─> gerarAlertaParaPlano()                                    │
│       └─> criarOrdemServicoParaManutencao()                     │
│           └─> OrdemServicoService.criar()                       │
│               └─> NotificacaoEventPublisher.publicarOSCriada()  │
│                   └─> ENVIA NOTIFICAÇÃO #1                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ A cada 5 min: processarNotificacoesAgendadas()                  │
│   └─> Se plano tem agendamentosNotificacao                      │
│       └─> enviarNotificacaoAgendada()                           │
│           └─> criarOrdemServicoParaManutencao()                 │
│               └─> ENVIA NOTIFICAÇÃO #2 (DUPLICADA!)             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 8h30: verificarManutencaoVencidas()                             │
│   └─> gerarAlertaParaPlano()                                    │
│       └─> criarOrdemServicoParaManutencao()                     │
│           └─> ENVIA NOTIFICAÇÃO #3 (DUPLICADA!)                 │
└─────────────────────────────────────────────────────────────────┘
```

### Fluxo ESPERADO:

```
┌─────────────────────────────────────────────────────────────────┐
│ Plano com agendamentos manuais:                                 │
│   processarNotificacoesAgendadas() a cada 5 min                 │
│   └─> Verifica se está na hora E se não foi enviado             │
│       └─> ENVIA NOTIFICAÇÃO ÚNICA                               │
│           └─> Marca agendamento como enviado                    │
│           └─> Atualiza ultimoAlertaEnviadoEm                    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ Plano SEM agendamentos manuais:                                 │
│   verificarManutencoesPendentes() 1x/dia                        │
│   └─> Verifica antecedência E se não alertou nas últimas 24h    │
│       └─> ENVIA NOTIFICAÇÃO ÚNICA                               │
│           └─> Atualiza ultimoAlertaEnviadoEm                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Correções Necessárias

### Prioridade ALTA (Corrigir imediatamente):

1. **BUG 1** - Corrigir condição invertida na linha 245
2. **BUG 2** - Separar fluxos: planos COM agendamentos vs planos automáticos
3. **BUG 3** - Limitar alertas de vencimento

### Prioridade MÉDIA:

4. **BUG 4** - Adicionar verificação de duplicata em `gerarAlertaLembrete()`
5. **BUG 5** - Garantir atomicidade nas atualizações
6. **BUG 6** - Verificar OS existente antes de criar nova

---

## Solução Recomendada

### Opção 1: Simplificar (Recomendada)

Remover a criação automática de OS e usar APENAS alertas:

```java
// Em gerarAlertaParaPlano(), NÃO criar OS, apenas alerta
private boolean gerarAlertaParaPlano(PlanoManutencaoPreventiva plano, TipoAlerta tipoAlerta) {
    // Verifica se já existe alerta pendente
    if (alertaRepository.existsAlertaPendente(plano.getId(), tipoAlerta)) {
        return false;
    }

    // Cria APENAS o alerta, não a OS
    AlertaManutencao alerta = AlertaManutencao.builder()
        .oficina(plano.getOficina())
        .plano(plano)
        // ...
        .build();

    alertaRepository.save(alerta);
    plano.setUltimoAlertaEnviadoEm(LocalDateTime.now());
    planoRepository.save(plano);

    return true;
}
```

### Opção 2: Manter OS automática, mas corrigir fluxo

Se quiser manter a criação automática de OS:

1. Quando criar OS, marcar TODOS os agendamentos do plano como enviados
2. Verificar se o plano tem `agendamentosNotificacao` ANTES de criar OS automática
3. Se tiver, pular - deixa o job de agendamentos processar

---

## Correções Aplicadas (2026-01-26)

### ✅ BUG 1: Condição invertida CORRIGIDA
- Arquivo: `ManutencaoPreventivaScheduler.java` linha 244-259
- Lógica agora verifica corretamente se está FORA da janela

### ✅ BUG 2: Duplicação CORRIGIDA
- Arquivo: `ManutencaoPreventivaScheduler.java` linha 361-370
- Quando OS é criada automaticamente, marca todos os agendamentos como enviados

### ✅ BUG 3: Spam de vencimento CORRIGIDO
- Arquivo: `ManutencaoPreventivaScheduler.java` linha 110-121
- Intervalo aumentado de 7 para 30 dias

### ✅ BUG 4: Duplicata de lembrete CORRIGIDA
- Arquivo: `ManutencaoPreventivaScheduler.java` linha 464-469
- Verifica se já existe alerta pendente antes de criar

---

## Próximos Passos

1. [x] Corrigir BUG 1 (condição invertida) ✅
2. [x] Corrigir BUG 2 (duplicação) ✅
3. [x] Corrigir BUG 3 (spam vencimento) ✅
4. [x] Corrigir BUG 4 (lembrete duplicado) ✅
5. [ ] Reiniciar backend e testar
6. [ ] Monitorar logs por 24h
7. [ ] Validar que apenas 1 notificação é enviada por evento
