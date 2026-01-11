# Modelo Híbrido de Cobrança de Mão de Obra + Peças Avulsas

## Visão Geral

Este documento descreve a implementação do **Modelo Híbrido de Cobrança de Mão de Obra** e o suporte a **Peças Avulsas** no sistema PitStop. Essas funcionalidades permitem maior flexibilidade na criação de orçamentos e ordens de serviço.

---

## 1. Modelo Híbrido de Mão de Obra

### 1.1 Conceito

O sistema agora suporta dois modelos de cobrança de mão de obra:

| Modelo | Descrição | Quando Usar |
|--------|-----------|-------------|
| **VALOR_FIXO** | Valor definido no momento da criação do orçamento | Serviços com escopo bem definido (ex: troca de óleo, alinhamento) |
| **POR_HORA** | Valor calculado com base nas horas trabalhadas | Serviços complexos onde o tempo pode variar (ex: diagnóstico de falhas, reparos extensos) |

### 1.2 Fluxo do Modelo VALOR_FIXO

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Criar OS       │────▶│  Cliente        │────▶│  Finalizar      │
│  (valor fixo)   │     │  Aprova         │     │  (mesmo valor)  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
     R$ 500                  R$ 500                   R$ 500
```

1. Atendente cria OS com `tipoCobrancaMaoObra = VALOR_FIXO`
2. Define `valorMaoObra = R$ 500`
3. Cliente aprova o orçamento com valor fixo
4. Mecânico finaliza o serviço
5. Valor final da mão de obra = R$ 500 (exatamente como aprovado)

### 1.3 Fluxo do Modelo POR_HORA

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Criar OS       │────▶│  Cliente        │────▶│  Finalizar      │
│  (estimativa)   │     │  Aprova LIMITE  │     │  (horas reais)  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
   Estimativa: 3h          Limite: 5h              Trabalhadas: 4h
   R$ 240 - R$ 400         Máx: R$ 400             Final: R$ 320
```

1. Atendente cria OS com `tipoCobrancaMaoObra = POR_HORA`
2. Define:
   - `tempoEstimadoHoras = 3` (estimativa)
   - `limiteHorasAprovado = 5` (máximo que o cliente aprova)
3. Sistema captura `valorHoraSnapshot` da configuração da oficina (ex: R$ 80/h)
4. Cliente vê no orçamento:
   - Estimativa mínima: 3h × R$ 80 = R$ 240
   - **Limite aprovado: 5h × R$ 80 = R$ 400**
5. Mecânico finaliza informando horas trabalhadas (ex: 4h)
6. Sistema calcula: 4h × R$ 80 = R$ 320

### 1.4 Validação do Limite de Horas

**IMPORTANTE**: Se as horas trabalhadas excederem o limite aprovado, a finalização é bloqueada.

```
┌─────────────────────────────────────────────────────────────┐
│  ERRO: Limite de Horas Excedido                             │
│                                                             │
│  Horas trabalhadas (6h) excedem o limite aprovado (5h).     │
│  Necessário nova aprovação do cliente.                      │
└─────────────────────────────────────────────────────────────┘
```

Isso garante que o cliente nunca seja cobrado mais do que aprovou.

---

## 2. Peças Avulsas

### 2.1 Conceito

O sistema agora suporta três origens de peças:

| Origem | Descrição | Afeta Estoque? |
|--------|-----------|----------------|
| **ESTOQUE** | Peça do inventário da oficina | Sim - baixa automática |
| **AVULSA** | Peça comprada externamente para o serviço | Não |
| **CLIENTE** | Peça fornecida pelo próprio cliente | Não |

### 2.2 Comportamento por Origem

#### ESTOQUE
- Requer seleção da peça no autocomplete
- Valida estoque disponível no momento da criação
- Baixa automática do estoque na finalização da OS
- Utiliza preço de venda cadastrado

#### AVULSA
- Não requer seleção de peça do estoque
- Requer descrição detalhada (mínimo 10 caracteres)
- Valor unitário informado manualmente
- Não afeta o estoque

#### CLIENTE
- Peça trazida pelo cliente
- Requer descrição detalhada
- Geralmente com valor zero (ou apenas taxa de instalação)
- Não afeta o estoque

### 2.3 Validações

```typescript
// Peça do ESTOQUE
{
  tipo: 'PECA',
  origemPeca: 'ESTOQUE',
  pecaId: 'uuid-da-peca',      // Obrigatório
  descricao: 'Filtro de óleo', // Preenchido automaticamente
  valorUnitario: 45.00         // Do cadastro
}

// Peça AVULSA
{
  tipo: 'PECA',
  origemPeca: 'AVULSA',
  pecaId: null,                           // Não tem
  descricao: 'Sensor de temperatura XYZ', // Mínimo 10 chars
  valorUnitario: 180.00                   // Informado manualmente
}

// Peça do CLIENTE
{
  tipo: 'PECA',
  origemPeca: 'CLIENTE',
  pecaId: null,
  descricao: 'Farol LED trazido pelo cliente',
  valorUnitario: 0.00  // Apenas instalação
}
```

---

## 3. Estrutura do Banco de Dados

### 3.1 Tabela `oficinas`

```sql
-- Nova coluna adicionada (V055)
ALTER TABLE oficinas ADD COLUMN valor_hora DECIMAL(10,2) DEFAULT 80.00;
```

### 3.2 Tabela `ordens_servico`

```sql
-- Novas colunas adicionadas (V056)
ALTER TABLE ordens_servico ADD COLUMN tipo_cobranca_mao_obra VARCHAR(20) DEFAULT 'VALOR_FIXO';
ALTER TABLE ordens_servico ADD COLUMN tempo_estimado_horas DECIMAL(5,2);
ALTER TABLE ordens_servico ADD COLUMN limite_horas_aprovado DECIMAL(5,2);
ALTER TABLE ordens_servico ADD COLUMN horas_trabalhadas DECIMAL(5,2);
ALTER TABLE ordens_servico ADD COLUMN valor_hora_snapshot DECIMAL(10,2);

-- Constraints
CONSTRAINT chk_tipo_cobranca CHECK (tipo_cobranca_mao_obra IN ('VALOR_FIXO', 'POR_HORA'))
CONSTRAINT chk_horas_range CHECK (horas_trabalhadas IS NULL OR (horas_trabalhadas >= 0.5 AND horas_trabalhadas <= 100))
```

### 3.3 Tabela `item_os`

```sql
-- Nova coluna adicionada (V057)
ALTER TABLE item_os ADD COLUMN origem_peca VARCHAR(20);

-- Constraint
CONSTRAINT chk_origem_peca CHECK (origem_peca IS NULL OR origem_peca IN ('ESTOQUE', 'AVULSA', 'CLIENTE'))
```

---

## 4. API Endpoints

### 4.1 Criar OS com Modelo Híbrido

**POST** `/api/ordens-servico`

```json
// VALOR_FIXO
{
  "veiculoId": "uuid",
  "usuarioId": "uuid",
  "problemasRelatados": "Barulho no motor",
  "tipoCobrancaMaoObra": "VALOR_FIXO",
  "valorMaoObra": 350.00,
  "itens": [...]
}

// POR_HORA
{
  "veiculoId": "uuid",
  "usuarioId": "uuid",
  "problemasRelatados": "Diagnóstico completo",
  "tipoCobrancaMaoObra": "POR_HORA",
  "tempoEstimadoHoras": 3.0,
  "limiteHorasAprovado": 5.0,
  "itens": [...]
}
```

### 4.2 Criar Item com Origem de Peça

```json
// Item PECA do ESTOQUE
{
  "tipo": "PECA",
  "origemPeca": "ESTOQUE",
  "pecaId": "uuid-da-peca",
  "descricao": "Filtro de óleo",
  "quantidade": 1,
  "valorUnitario": 45.00
}

// Item PECA AVULSA
{
  "tipo": "PECA",
  "origemPeca": "AVULSA",
  "descricao": "Sensor de temperatura modelo ABC-123",
  "quantidade": 1,
  "valorUnitario": 180.00
}

// Item SERVICO (não tem origemPeca)
{
  "tipo": "SERVICO",
  "descricao": "Troca de óleo",
  "quantidade": 1,
  "valorUnitario": 50.00
}
```

### 4.3 Finalizar OS com Horas (POR_HORA)

**POST** `/api/ordens-servico/{id}/finalizar`

```json
{
  "horasTrabalhadas": 4.0,
  "observacoesFinais": "Serviço concluído com sucesso"
}
```

**Resposta de Sucesso (200)**:
```json
{
  "id": "uuid",
  "numero": 123,
  "status": "FINALIZADO",
  "tipoCobrancaMaoObra": "POR_HORA",
  "horasTrabalhadas": 4.0,
  "valorHoraSnapshot": 80.00,
  "valorMaoObra": 320.00,
  "valorFinal": 420.00
}
```

**Resposta de Erro - Limite Excedido (400)**:
```json
{
  "type": "about:blank",
  "title": "Limite de Horas Excedido",
  "status": 400,
  "detail": "Horas trabalhadas (6.0) excedem o limite aprovado pelo cliente (5.0)",
  "instance": "/api/ordens-servico/uuid/finalizar",
  "horasTrabalhadas": 6.0,
  "limiteAprovado": 5.0
}
```

---

## 5. Interface do Usuário

### 5.1 Formulário de Criação de OS

O formulário agora exibe um seletor de tipo de cobrança:

```
┌─────────────────────────────────────────────────────────────┐
│  Tipo de Cobrança de Mão de Obra                            │
│                                                             │
│  ┌─────────────────────┐  ┌─────────────────────┐          │
│  │ [$] Valor Fixo      │  │ [⏱] Por Hora        │          │
│  │     Valor definido  │  │     Por horas       │          │
│  └─────────────────────┘  └─────────────────────┘          │
│                                                             │
│  [Se VALOR_FIXO]                                            │
│  Valor Mão de Obra: [___________] R$                        │
│                                                             │
│  [Se POR_HORA]                                              │
│  Tempo Estimado (horas): [___] h                            │
│  Limite de Horas Aprovado: [___] h                          │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ ⏱ Cobrança por Hora                                 │   │
│  │ Valor/hora: R$ 80,00                                │   │
│  │                                                     │   │
│  │ Estimativa Mínima    Limite Aprovado (máx)          │   │
│  │ R$ 240,00            R$ 400,00                      │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Página de Aprovação do Cliente

Para orçamentos POR_HORA, o cliente vê claramente o limite que está aprovando:

```
┌─────────────────────────────────────────────────────────────┐
│  ⏱ Mão de Obra por Hora                                     │
│                                                             │
│  Valor/hora:        R$ 80,00                                │
│  Tempo estimado:    3h                                      │
│  Estimativa:        R$ 240,00                               │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ ⚠️ Limite que você está aprovando:                   │   │
│  │                                                     │   │
│  │ Até 5h = R$ 400,00                                  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Peças:             R$ 150,00                               │
│  ─────────────────────────────                              │
│  TOTAL:             R$ 550,00 (estimativa)                  │
│                     até R$ 550,00 (máximo)                  │
│                                                             │
│  [✓ Aprovar Orçamento]    [✗ Rejeitar]                     │
└─────────────────────────────────────────────────────────────┘
```

### 5.3 Formulário de Itens com Origem de Peça

Quando o tipo do item é "Peça", um seletor de origem é exibido:

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Item #1                                                           [🗑] │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Tipo         Origem       Descrição/Peça      Qtd.  Valor Unit. Desc. │
│  ┌─────────┐  ┌─────────┐  ┌──────────────┐   ┌───┐  ┌────────┐  ┌───┐ │
│  │ Peça  ▼ │  │Estoque▼ │  │ [Autocomplete]│   │ 1 │  │ 45,00  │  │ 0 │ │
│  └─────────┘  └─────────┘  └──────────────┘   └───┘  └────────┘  └───┘ │
│               Do inventário                                             │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│  Item #2                                                           [🗑] │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Tipo         Origem       Descrição da Peça  Qtd.  Valor Unit. Desc.  │
│  ┌─────────┐  ┌─────────┐  ┌──────────────┐   ┌───┐  ┌────────┐  ┌───┐ │
│  │ Peça  ▼ │  │Avulsa ▼ │  │ Sensor XYZ.. │   │ 1 │  │ 180,00 │  │ 0 │ │
│  └─────────┘  └─────────┘  └──────────────┘   └───┘  └────────┘  └───┘ │
│               Compra externa                                            │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│  Item #3                                                           [🗑] │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Tipo         Origem       Descrição da Peça  Qtd.  Valor Unit. Desc.  │
│  ┌─────────┐  ┌─────────┐  ┌──────────────┐   ┌───┐  ┌────────┐  ┌───┐ │
│  │ Peça  ▼ │  │Cliente▼ │  │ Farol LED .. │   │ 1 │  │  0,00  │  │ 0 │ │
│  └─────────┘  └─────────┘  └──────────────┘   └───┘  └────────┘  └───┘ │
│               Cliente trouxe                                            │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**Comportamento por Origem:**

| Origem | Campo Descrição | Valor Unitário |
|--------|----------------|----------------|
| Estoque | Autocomplete de peças (preenchimento automático) | Do cadastro |
| Avulsa | Input de texto livre (mín. 10 caracteres) | Manual |
| Cliente | Input de texto livre (mín. 10 caracteres) | Zerado automaticamente |

### 5.4 Modal de Finalização (POR_HORA)

Quando a OS é POR_HORA, o mecânico/atendente vê um modal para informar as horas:

```
┌─────────────────────────────────────────────────────────────┐
│  ✓ Finalizar OS #123                                        │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ ⏱ Cobrança por Hora                                 │   │
│  │ Valor/hora: R$ 80,00                                │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Horas Trabalhadas *                                        │
│  [____4.0____] h                                            │
│                                                             │
│  Tempo estimado: 3h    Limite aprovado: 5h                  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Resumo Financeiro                                   │   │
│  │                                                     │   │
│  │ Peças:                              R$ 150,00       │   │
│  │ Mão de Obra (4h):                   R$ 320,00       │   │
│  │ ─────────────────────────────────────────────       │   │
│  │ Total:                              R$ 470,00       │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Observações Finais (opcional)                              │
│  [_________________________________________________]       │
│                                                             │
│                        [Cancelar]  [Finalizar OS]           │
└─────────────────────────────────────────────────────────────┘
```

---

## 6. Regras de Negócio

### 6.1 Criação de OS

| Campo | VALOR_FIXO | POR_HORA |
|-------|------------|----------|
| `tipoCobrancaMaoObra` | Obrigatório | Obrigatório |
| `valorMaoObra` | Obrigatório (≥ 0) | Ignorado |
| `tempoEstimadoHoras` | Ignorado | Obrigatório (0.5 - 100) |
| `limiteHorasAprovado` | Ignorado | Obrigatório (≥ tempoEstimadoHoras) |
| `valorHoraSnapshot` | Não capturado | Capturado da oficina |

### 6.2 Aprovação pelo Cliente

- **VALOR_FIXO**: Cliente aprova o valor exato da mão de obra
- **POR_HORA**: Cliente aprova o **limite máximo** de horas

### 6.3 Finalização

| Modelo | Endpoint | Corpo | Cálculo |
|--------|----------|-------|---------|
| VALOR_FIXO | `PATCH /{id}/finalizar` | Nenhum | Mantém `valorMaoObra` |
| POR_HORA | `POST /{id}/finalizar` | `{ horasTrabalhadas }` | `horasTrabalhadas × valorHoraSnapshot` |

### 6.4 Baixa de Estoque

A baixa de estoque ocorre **apenas** para itens com `origemPeca = ESTOQUE`:

```java
// MovimentacaoEstoqueService.java
List<ItemOS> itensPecaEstoque = itens.stream()
    .filter(item -> item.getTipo() == TipoItem.PECA)
    .filter(item -> item.getOrigemPeca() == OrigemPeca.ESTOQUE)
    .filter(item -> item.getPecaId() != null)
    .toList();
```

---

## 7. Arquivos Modificados

### Backend

| Arquivo | Modificação |
|---------|-------------|
| `V055__add_valor_hora_to_oficinas.sql` | Nova migration |
| `V056__add_modelo_cobranca_os.sql` | Nova migration |
| `V057__add_origem_peca_item_os.sql` | Nova migration |
| `TipoCobrancaMaoObra.java` | Novo enum |
| `OrigemPeca.java` | Novo enum |
| `Oficina.java` | Adicionado `valorHora` |
| `OrdemServico.java` | Campos do modelo híbrido |
| `ItemOS.java` | Adicionado `origemPeca` |
| `CreateOrdemServicoDTO.java` | Campos do modelo híbrido |
| `CreateItemOSDTO.java` | Adicionado `origemPeca` |
| `OrdemServicoResponseDTO.java` | Campos do modelo híbrido |
| `ItemOSResponseDTO.java` | Adicionado `origemPeca` |
| `FinalizarOSDTO.java` | Novo DTO |
| `OrdemServicoService.java` | Lógica do modelo híbrido |
| `OrdemServicoController.java` | Endpoint de finalização |
| `OrdemServicoMapper.java` | Mapeamentos |
| `MovimentacaoEstoqueService.java` | Filtro por origem |
| `LimiteHorasExcedidoException.java` | Nova exception |
| `GlobalExceptionHandler.java` | Handler da exception |

### Frontend

| Arquivo | Modificação |
|---------|-------------|
| `types/index.ts` | Enums e interfaces |
| `utils/validation.ts` | Schemas Zod |
| `hooks/useOrdensServico.ts` | Hook `useFinalizarComHoras` |
| `services/ordemServicoService.ts` | Método `finalizarComHoras` |
| `components/FinalizarOSModal.tsx` | Novo componente |
| `pages/OrdemServicoFormPage.tsx` | UI do modelo híbrido |
| `pages/AprovarOrcamentoPage.tsx` | Exibição POR_HORA |

---

## 8. Testes Manuais Recomendados

### 8.1 VALOR_FIXO

1. Criar OS com mão de obra fixa R$ 500
2. Aprovar orçamento
3. Iniciar trabalho
4. Finalizar (sem informar horas)
5. Verificar que valor final = R$ 500

### 8.2 POR_HORA

1. Criar OS com:
   - Tempo estimado: 3h
   - Limite aprovado: 5h
   - Valor/hora da oficina: R$ 80
2. Aprovar orçamento (cliente vê limite de R$ 400)
3. Iniciar trabalho
4. Finalizar com 4h trabalhadas
5. Verificar que mão de obra = R$ 320

### 8.3 Limite Excedido

1. Criar OS POR_HORA com limite 5h
2. Aprovar e iniciar
3. Tentar finalizar com 6h
4. Verificar mensagem de erro

### 8.4 Peças por Origem

1. Adicionar peça do ESTOQUE
2. Adicionar peça AVULSA
3. Adicionar peça do CLIENTE
4. Finalizar e entregar OS
5. Verificar que apenas peça ESTOQUE teve baixa

---

## 9. Considerações Futuras

1. **Buscar valor/hora da oficina dinamicamente** no frontend (atualmente hardcoded como R$ 80)
2. **Relatórios** de rentabilidade por tipo de cobrança
3. **Alertas** quando horas se aproximam do limite

---

*Documento criado em: Janeiro 2026*
*Versão: 1.0*


🔴 CRÍTICOS (Afetam funcionalidade)

1. Modal de Finalização para POR_HORA
   - Não existe FinalizarOSModal.tsx para informar horas trabalhadas
   - Mecânico/atendente não consegue finalizar OS com cobrança por hora
2. Detalhes de POR_HORA não exibidos
   - OrdemServicoDetailPage.tsx não mostra: tempoEstimadoHoras, limiteHorasAprovado, horasTrabalhadas, valorHoraSnapshot
3. Origem da peça não exibida
   - ItemOSTable.tsx não diferencia ESTOQUE/AVULSA/CLIENTE
   - PDF não mostra origem

🟡 IMPORTANTES (Melhorias de UX)

4. Notificação de rejeição
   - Oficina não recebe aviso quando cliente rejeita orçamento
5. Link de aprovação dinâmico
   - Hardcoded como localhost:5173
   - Deveria usar variável de ambiente
6. PDF por email na finalização
   - Cliente não recebe PDF automaticamente
7. Status AGUARDANDO_PECA
   - Não há botão na UI para pausar serviço aguardando peça
8. Timeline completa
   - Não há histórico de mudanças de status (quem, quando)

🟢 MELHORIAS MENORES

9. Tipo de item no PDF - Diferenciar PECA vs SERVICO
10. Validação de desconto - Impedir desconto > valor total
11. Informações de POR_HORA no PDF - Mostrar valor/hora e horas trabalhadas

  ---
PRÓXIMOS PASSOS RECOMENDADOS

Quer que eu implemente alguma dessas correções? Sugiro começar pelos itens críticos:

1. Criar FinalizarOSModal.tsx - Modal para informar horas trabalhadas (POR_HORA)
2. Atualizar OrdemServicoDetailPage.tsx - Exibir informações do modelo híbrido
3. Atualizar ItemOSTable.tsx - Mostrar origem da peça