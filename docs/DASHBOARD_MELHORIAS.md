# Dashboard - Melhorias Planejadas

> Documento criado em: 2026-01-17
> Status: Pendente implementação

## Situação Atual

O dashboard atual possui os seguintes componentes:

### Cards de Estatísticas (4 cards principais)
| Card | Descrição | Acesso |
|------|-----------|--------|
| Total de Clientes | Clientes ativos | Todos |
| Total de Veículos | Veículos cadastrados | Todos |
| OS Ativas | OS em andamento (exceto canceladas/entregues) | Todos |
| Faturamento do Mês | Total de OS entregues no mês | Todos |

### Cards Extras (3 cards - ADMIN/GERENTE)
| Card | Descrição |
|------|-----------|
| Ticket Médio (Mês) | Valor médio por OS |
| Valor Total Estoque | Somatório: quantidade × valorCusto |
| Peças Estoque Baixo | Peças com quantidade ≤ mínima |

### Gráficos
| Gráfico | Tipo | Descrição |
|---------|------|-----------|
| OS por Status | Pizza | Distribuição dos 7 status de OS |
| Faturamento Mensal | Barras | Últimos 6 meses |

### Tabela
- **OS Recentes**: Últimas 10 ordens de serviço

---

## Melhorias Propostas

### 1. Módulo Financeiro

#### 1.1 Card: Pagamentos Pendentes
**Prioridade:** Alta

```java
// Backend - DashboardService.java
public PagamentosPendentesDTO getPagamentosPendentes() {
    UUID oficinaId = TenantContext.getTenantId();
    Long count = pagamentoRepository.countByOficinaIdAndStatus(oficinaId, StatusPagamento.PENDENTE);
    BigDecimal valor = pagamentoRepository.sumValorByOficinaIdAndStatus(oficinaId, StatusPagamento.PENDENTE);
    return new PagamentosPendentesDTO(count, valor);
}
```

```typescript
// Frontend - DTO
interface PagamentosPendentesDTO {
  quantidade: number;
  valorTotal: number;
}
```

**Endpoint:** `GET /api/dashboard/financeiro/pagamentos-pendentes`

---

#### 1.2 Card: Total Recebido (Mês)
**Prioridade:** Média

```java
// Backend - PagamentoRepository.java (adicionar)
@Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p " +
       "WHERE p.oficina.id = :oficinaId " +
       "AND p.status = 'PAGO' " +
       "AND MONTH(p.dataPagamento) = MONTH(CURRENT_DATE) " +
       "AND YEAR(p.dataPagamento) = YEAR(CURRENT_DATE)")
BigDecimal sumRecebidoNoMes(@Param("oficinaId") UUID oficinaId);
```

**Endpoint:** `GET /api/dashboard/financeiro/recebido-mes`

---

#### 1.3 Gráfico: Pagamentos por Tipo
**Prioridade:** Alta

```java
// Backend - DTO
public record PagamentoPorTipoDTO(
    TipoPagamento tipo,  // DINHEIRO, PIX, CARTAO_CREDITO, etc.
    Long quantidade,
    BigDecimal valorTotal
) {}

// Repository
@Query("SELECT new PagamentoPorTipoDTO(p.tipo, COUNT(p), SUM(p.valor)) " +
       "FROM Pagamento p " +
       "WHERE p.oficina.id = :oficinaId " +
       "AND p.status = 'PAGO' " +
       "AND p.dataPagamento >= :dataInicio " +
       "GROUP BY p.tipo")
List<PagamentoPorTipoDTO> countByTipo(UUID oficinaId, LocalDate dataInicio);
```

**Endpoint:** `GET /api/dashboard/financeiro/por-tipo`

**Frontend:** Gráfico de pizza ou barras horizontais

---

#### 1.4 Card: Taxa de Conversão Orçamento → Aprovado
**Prioridade:** Baixa

```java
// Lógica
Long orcamentos = osRepository.countByStatusAndMes(ORCAMENTO, mesAtual);
Long aprovados = osRepository.countByStatusAndMes(APROVADO, mesAtual);
Double taxa = (aprovados * 100.0) / orcamentos;
```

**Endpoint:** `GET /api/dashboard/financeiro/taxa-conversao`

---

### 2. Módulo Manutenção Preventiva

#### 2.1 Card: Planos de Manutenção Ativos
**Prioridade:** Alta

```java
// Backend - PlanoManutencaoRepository.java (já existe)
@Query("SELECT COUNT(p) FROM PlanoManutencaoPreventiva p " +
       "WHERE p.oficina.id = :oficinaId " +
       "AND p.status = 'ATIVO' " +
       "AND p.ativo = true")
Long countAtivos(@Param("oficinaId") UUID oficinaId);
```

**Endpoint:** `GET /api/dashboard/manutencao/planos-ativos`

---

#### 2.2 Card: Alertas de Manutenção Pendentes
**Prioridade:** Alta

```java
// Backend - AlertaManutencaoRepository.java (já existe)
@Query("SELECT COUNT(a) FROM AlertaManutencao a " +
       "WHERE a.oficina.id = :oficinaId " +
       "AND a.status = 'PENDENTE'")
Long countPendentes(@Param("oficinaId") UUID oficinaId);
```

**Endpoint:** `GET /api/dashboard/manutencao/alertas-pendentes`

---

#### 2.3 Card/Lista: Próximas Manutenções (7 dias)
**Prioridade:** Média

```java
// Backend - DTO
public record ProximaManutencaoDTO(
    UUID planoId,
    String nomeVeiculo,
    String placa,
    String tipoManutencao,
    LocalDate dataPrevisao,
    Integer kmPrevisao
) {}

// Repository
@Query("SELECT new ProximaManutencaoDTO(...) FROM PlanoManutencaoPreventiva p " +
       "WHERE p.oficina.id = :oficinaId " +
       "AND p.status = 'ATIVO' " +
       "AND p.proximaPrevisaoData <= :dataLimite " +
       "ORDER BY p.proximaPrevisaoData ASC")
List<ProximaManutencaoDTO> findProximasManutencoes(UUID oficinaId, LocalDate dataLimite);
```

**Endpoint:** `GET /api/dashboard/manutencao/proximas?dias=7`

**Frontend:** Lista com 5 itens ou card expansível

---

#### 2.4 Gráfico: Status dos Planos
**Prioridade:** Baixa

```java
// Distribuição: ATIVO, PAUSADO, VENCIDO, CONCLUIDO
@Query("SELECT p.status, COUNT(p) FROM PlanoManutencaoPreventiva p " +
       "WHERE p.oficina.id = :oficinaId " +
       "GROUP BY p.status")
List<Object[]> countByStatus(UUID oficinaId);
```

**Endpoint:** `GET /api/dashboard/manutencao/por-status`

---

### 3. Módulo Estoque

#### 3.1 Card: Peças com Estoque Zero
**Prioridade:** Média

```java
// Backend - PecaRepository.java (já existe método similar)
@Query("SELECT COUNT(p) FROM Peca p " +
       "WHERE p.oficina.id = :oficinaId " +
       "AND p.ativo = true " +
       "AND p.quantidadeAtual = 0")
Long countEstoqueZero(@Param("oficinaId") UUID oficinaId);
```

**Endpoint:** `GET /api/dashboard/estoque/zeradas`

---

#### 3.2 Card: Peças Sem Localização
**Prioridade:** Baixa

```java
// Já existe em PecaRepository
Long countByOficinaIdAndLocalArmazenamentoIsNullAndAtivoTrue(UUID oficinaId);
```

**Endpoint:** `GET /api/dashboard/estoque/sem-localizacao`

---

#### 3.3 Gráfico: Movimentações (30 dias)
**Prioridade:** Baixa

```java
// Entradas vs Saídas vs Ajustes
@Query("SELECT m.tipo, COUNT(m) FROM MovimentacaoEstoque m " +
       "WHERE m.oficina.id = :oficinaId " +
       "AND m.dataMovimentacao >= :dataInicio " +
       "GROUP BY m.tipo")
List<Object[]> countMovimentacoesPorTipo(UUID oficinaId, LocalDateTime dataInicio);
```

**Endpoint:** `GET /api/dashboard/estoque/movimentacoes`

---

### 4. Módulo Notificações

#### 4.1 Card: Notificações Enviadas (Mês)
**Prioridade:** Baixa

```java
@Query("SELECT COUNT(h) FROM HistoricoNotificacao h " +
       "WHERE h.oficina.id = :oficinaId " +
       "AND h.enviadoEm >= :dataInicio")
Long countEnviadasNoMes(UUID oficinaId, LocalDateTime dataInicio);
```

**Endpoint:** `GET /api/dashboard/notificacoes/enviadas`

---

#### 4.2 Gráfico: Notificações por Canal
**Prioridade:** Baixa

```java
// Distribuição: WHATSAPP, EMAIL, TELEGRAM, SMS
@Query("SELECT h.canal, COUNT(h) FROM HistoricoNotificacao h " +
       "WHERE h.oficina.id = :oficinaId " +
       "AND h.enviadoEm >= :dataInicio " +
       "GROUP BY h.canal")
List<Object[]> countPorCanal(UUID oficinaId, LocalDateTime dataInicio);
```

---

### 5. Melhorias Gerais

#### 5.1 Comparação com Mês Anterior
**Prioridade:** Média

Adicionar percentual de variação nos cards existentes:

```typescript
interface StatCardProps {
  titulo: string;
  valor: number | string;
  icone: React.ReactNode;
  variacao?: number;  // +15 ou -10 (percentual)
  cor?: 'green' | 'red' | 'gray';
}

// Exibição
// Faturamento: R$ 28.750,50
// ↑ +15% vs mês anterior
```

```java
// Backend - calcular variação
BigDecimal faturamentoAtual = calcularFaturamentoMes(mesAtual);
BigDecimal faturamentoAnterior = calcularFaturamentoMes(mesAnterior);
Double variacao = ((faturamentoAtual - faturamentoAnterior) / faturamentoAnterior) * 100;
```

---

#### 5.2 Filtro de Período
**Prioridade:** Baixa

Adicionar seletor de período no topo do dashboard:

```typescript
type Periodo = 'hoje' | '7dias' | '30dias' | 'mes' | 'trimestre' | 'ano' | 'custom';

interface FiltroProps {
  periodo: Periodo;
  dataInicio?: Date;
  dataFim?: Date;
}
```

---

## Arquitetura de Implementação

### Backend

```
src/main/java/com/pitstop/dashboard/
├── controller/
│   └── DashboardController.java      // Expandir com novos endpoints
├── dto/
│   ├── DashboardStats.java           // (existente)
│   ├── FinanceiroStatsDTO.java       // NOVO
│   ├── ManutencaoStatsDTO.java       // NOVO
│   ├── EstoqueStatsDTO.java          // NOVO
│   ├── PagamentoPorTipoDTO.java      // NOVO
│   └── ProximaManutencaoDTO.java     // NOVO
└── service/
    └── DashboardService.java         // Expandir com novos métodos
```

### Frontend

```
frontend/src/features/dashboard/
├── components/
│   ├── StatCard.tsx                  // (existente - adicionar variação)
│   ├── OSStatusPieChart.tsx          // (existente)
│   ├── FaturamentoBarChart.tsx       // (existente)
│   ├── RecentOSTable.tsx             // (existente)
│   ├── PagamentosPorTipoChart.tsx    // NOVO
│   ├── ManutencaoAlertsCard.tsx      // NOVO
│   ├── ProximasManutencoesCard.tsx   // NOVO
│   └── PeriodoFilter.tsx             // NOVO (baixa prioridade)
├── hooks/
│   ├── useDashboardStats.ts          // (existente)
│   ├── useFinanceiroStats.ts         // NOVO
│   ├── useManutencaoStats.ts         // NOVO
│   └── useEstoqueStats.ts            // NOVO
├── types/
│   └── index.ts                      // Expandir com novos tipos
└── pages/
    └── DashboardPage.tsx             // Expandir layout
```

---

## Ordem de Implementação Sugerida

### Sprint 1 - Alta Prioridade
1. [ ] Card: Pagamentos Pendentes
2. [ ] Card: Planos Manutenção Ativos
3. [ ] Card: Alertas Pendentes
4. [ ] Gráfico: Pagamentos por Tipo

### Sprint 2 - Média Prioridade
5. [ ] Card: Total Recebido (Mês)
6. [ ] Card: Peças Estoque Zero
7. [ ] Lista: Próximas Manutenções (7 dias)
8. [ ] Melhoria: Comparação vs Mês Anterior

### Sprint 3 - Baixa Prioridade
9. [ ] Card: Taxa Conversão Orçamento
10. [ ] Card: Peças Sem Localização
11. [ ] Gráfico: Movimentações Estoque
12. [ ] Gráfico: Status Planos Manutenção
13. [ ] Filtro de Período Customizado

---

## Layout Proposto

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           DASHBOARD                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                    │
│  │ Clientes │ │ Veículos │ │ OS Ativas│ │Faturamento│  ← Cards atuais   │
│  │   125    │ │   203    │ │    18    │ │ R$28.750 │                    │
│  │          │ │          │ │          │ │  ↑ +15%  │  ← Com variação    │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘                    │
│                                                                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                    │
│  │Ticket Méd│ │ Estoque  │ │Est. Baixo│ │Pag.Pend. │  ← Cards extras    │
│  │ R$ 1.597 │ │R$ 45.320 │ │    12    │ │ R$ 3.200 │  ← NOVO            │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘                    │
│                                                                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                                 │
│  │Planos Man│ │ Alertas  │ │Est. Zero │  ← Cards Manutenção/Estoque     │
│  │    45    │ │    8     │ │    5     │  ← NOVOS                        │
│  └──────────┘ └──────────┘ └──────────┘                                 │
│                                                                          │
│  ┌─────────────────────────┐ ┌─────────────────────────┐                │
│  │   OS por Status (Pizza) │ │ Pagamentos por Tipo     │  ← NOVO        │
│  │                         │ │        (Pizza)          │                │
│  └─────────────────────────┘ └─────────────────────────┘                │
│                                                                          │
│  ┌─────────────────────────┐ ┌─────────────────────────┐                │
│  │ Faturamento 6 meses     │ │ Próximas Manutenções    │  ← NOVO        │
│  │       (Barras)          │ │       (Lista)           │                │
│  └─────────────────────────┘ └─────────────────────────┘                │
│                                                                          │
│  ┌──────────────────────────────────────────────────────┐               │
│  │              OS Recentes (Tabela)                     │               │
│  └──────────────────────────────────────────────────────┘               │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Permissões por Role

| Componente | MECANICO | ATENDENTE | GERENTE | ADMIN |
|------------|:--------:|:---------:|:-------:|:-----:|
| Cards básicos (4) | ✓ | ✓ | ✓ | ✓ |
| Cards extras | ✗ | ✗ | ✓ | ✓ |
| Pagamentos Pendentes | ✗ | ✓ | ✓ | ✓ |
| Planos Manutenção | ✗ | ✓ | ✓ | ✓ |
| Alertas Manutenção | ✗ | ✓ | ✓ | ✓ |
| Gráfico Pagamentos | ✗ | ✗ | ✓ | ✓ |
| Próximas Manutenções | ✗ | ✓ | ✓ | ✓ |
| Taxa Conversão | ✗ | ✗ | ✓ | ✓ |

---

## Observações

1. **Cache**: Usar cache Redis com TTL de 5 minutos para os novos endpoints
2. **WebSocket**: Considerar atualização real-time para alertas de manutenção
3. **Mobile**: Garantir que novos cards sejam responsivos
4. **Testes**: Criar testes unitários para os novos métodos do DashboardService
