# TAREFA: Implementar Diagnóstico Assistido por IA na Tela de Nova Ordem de Serviço

## 📋 CONTEXTO DO PROJETO

Você está trabalhando no **PitStop**, um sistema SaaS de gestão para oficinas mecânicas.

**API de IA:**
- Anthropic Claude
- Modelos: claude-haiku-4-5-20251001 (barato) e claude-sonnet-4-20250514 (avançado)

---

## 🎯 OBJETIVO

Adicionar funcionalidade de **diagnóstico assistido por IA** na tela de criação de Ordem de Serviço.

**Localização:** Tela "Nova Ordem de Serviço" (componente que cria OS)

**Comportamento:**
1. Usuário preenche campo "Problemas Relatados"
2. Botão "🤖 Gerar Diagnóstico com IA" aparece (só se houver texto)
3. Ao clicar, sistema analisa o problema usando IA
4. Exibe diagnóstico estruturado abaixo do campo
5. Mecânico pode aceitar sugestões ou editar manualmente

---

## 💰 REQUISITOS DE OTIMIZAÇÃO DE CUSTOS

Implemente as seguintes técnicas para **reduzir custos com API de IA em 70-80%**:

### 1. **Context Compression** (Resumo de Histórico)
- NÃO envie todo histórico de OS do veículo
- Envie apenas: últimas 2 OS + resumo estatístico das anteriores
- Formato compacto (pipe-separated, não JSON completo)
- Cache o histórico comprimido no Redis por 1 hora

### 2. **Model Routing** (Roteamento Inteligente)
- Primeira chamada: Claude Haiku classifica problema (SIMPLES ou COMPLEXO)
- Se SIMPLES: Haiku gera diagnóstico completo
- Se COMPLEXO: Sonnet faz análise detalhada
- ~70% dos casos resolvidos com modelo barato

### 3. **Pré-Validação** (Evitar Chamadas Desnecessárias)
- Antes de chamar IA, verifique palavras-chave comuns:
    - "troca de óleo" → template pré-definido
    - "revisão" → checklist padrão
    - "barulho freio" → diagnóstico comum de freio
- Só chame API se não encontrar match

### 4. **Cache Inteligente**
- Cache diagnósticos baseado em: hash(problema normalizado) + marca/modelo + faixa de quilometragem
- Problema similar em veículo similar = cache hit
- TTL: 24 horas
- Normalização: remove números específicos, datas, horários

### 5. **Formato Compacto** (Menos Tokens)
- Use formato pipe-separated em vez de JSON:
```
  CAUSAS: Vazamento óleo|85|ALTA; Junta queimada|60|MEDIA
  AÇÕES: Verificar nível óleo; Inspecionar radiador
```
- Economia: ~70% nos tokens de estrutura

### 6. **Prompt Modular**
- Separe em módulos independentes:
    1. Classificação (Haiku, 50 tokens max)
    2. Diagnóstico (Haiku ou Sonnet, 800 tokens max)
    3. Sugestão peças (opcional, só se necessário)

---

## 🏗️ ESTRUTURA DE IMPLEMENTAÇÃO

### **BACKEND**

Crie os seguintes componentes em `src/main/java/br/com/pitstop/`:

#### 1. `service/ia/DiagnosticoIAService.java`
**Responsabilidades:**
- Orquestrar todo o fluxo de diagnóstico
- Aplicar pré-validação
- Fazer cache lookup
- Rotear para modelo correto
- Comprimir contexto

**Métodos principais:**
```java
public DiagnosticoIA gerarDiagnostico(String problemasRelatados, UUID veiculoId)
public Optional<DiagnosticoIA> buscarCache(String problema, UUID veiculoId)
private ClassificacaoProblema classificarProblema(String problema) // Haiku
private DiagnosticoIA diagnosticarSimples(String problema, Veiculo veiculo) // Haiku
private DiagnosticoIA diagnosticarComplexo(String problema, HistoricoComprimido historico) // Sonnet
```

#### 2. `service/ia/ContextCompressionService.java`
**Responsabilidades:**
- Comprimir histórico de OS
- Gerar resumo estatístico
- Identificar padrões recorrentes

**Métodos principais:**
```java
@Cacheable("historico-comprimido")
public HistoricoComprimido comprimirHistorico(UUID veiculoId)
private String formatarOSRecentes(List<OrdemServico> recentes) // Formato compacto
private String gerarResumoEstatistico(List<OrdemServico> antigas)
```

#### 3. `service/ia/PreValidacaoService.java`
**Responsabilidades:**
- Detectar problemas comuns
- Retornar templates pré-definidos
- Evitar chamadas desnecessárias

**Métodos principais:**
```java
public Optional<DiagnosticoIA> tentarResolverSemIA(String problema)
private static final Map<String, DiagnosticoTemplate> PROBLEMAS_COMUNS
```

#### 4. `model/ia/DiagnosticoIA.java`
**Estrutura:**
```java
@Data @Builder
public class DiagnosticoIA {
    private String resumo;
    private List<CausaPossivel> causasPossiveis;
    private List<String> acoesRecomendadas;
    private List<PecaPropavel> pecasProvaveis;
    private String estimativaTempoReparo;
    private FaixaCusto custoEstimado;
    private boolean fromCache;
    private boolean fromTemplate;
}
```

#### 5. `controller/DiagnosticoIAController.java`
**Endpoint:**
```java
POST /api/v1/diagnostico-ia
Body: { "problemasRelatados": "string", "veiculoId": "uuid" }
Response: DiagnosticoIA
```

#### 6. `config/AnthropicConfig.java`
**Configuração:**
```java
@Bean
public Anthropic anthropicClient(@Value("${anthropic.api.key}") String apiKey) {
    return Anthropic.builder().apiKey(apiKey).build();
}
```

#### 7. `application.yml`
```yaml
anthropic:
  api:
    key: ${ANTHROPIC_API_KEY:sk-ant-xxxxx}
  models:
    haiku: claude-haiku-4-5-20251001
    sonnet: claude-sonnet-4-20250514
  cache:
    ttl: 24h
```

---

### **FRONTEND**

Localize e modifique o componente de criação de OS (provavelmente `NovaOrdemServico.tsx` ou similar).

#### 1. Criar `src/components/ia/DiagnosticoIA.tsx`
**Responsabilidades:**
- Botão para gerar diagnóstico
- Loading state
- Exibição estruturada do resultado
- Badges de probabilidade/urgência
- Aceitação de sugestões

**Estrutura:**
```tsx
interface DiagnosticoIAProps {
  problemasRelatados: string;
  veiculoId: string;
  onSugestaoAceita?: (diagnostico: DiagnosticoIA) => void;
}

export function DiagnosticoIA({ problemasRelatados, veiculoId, onSugestaoAceita }: Props) {
  // React Query mutation
  // UI com Shadcn components
  // Loading skeleton
  // Badges de probabilidade
  // Botão "Aceitar Sugestão"
}
```

#### 2. Criar `src/services/ia/diagnosticoIAService.ts`
```typescript
export const diagnosticoIAService = {
  async gerarDiagnostico(data: { problemasRelatados: string; veiculoId: string }) {
    const response = await api.post<DiagnosticoIA>('/diagnostico-ia', data);
    return response.data;
  }
};
```

#### 3. Integrar na Tela de Nova OS
**Localização:** Componente que contém o formulário de criação de OS

**Modificações:**
1. Importar componente `<DiagnosticoIA />`
2. Adicionar após o campo "Problemas Relatados"
3. Só renderizar se campo tiver pelo menos 20 caracteres
4. Ao aceitar sugestão, preencher campos:
    - Diagnóstico
    - Observações
    - (Opcional) Sugerir peças
```tsx
<FormField name="problemasRelatados">
  <Textarea ... />
</FormField>

{/* ADICIONAR AQUI */}
{problemasRelatados.length >= 20 && (
  <DiagnosticoIA
    problemasRelatados={problemasRelatados}
    veiculoId={veiculoId}
    onSugestaoAceita={(diagnostico) => {
      form.setValue('diagnostico', diagnostico.resumo);
      form.setValue('observacoes', diagnostico.acoesRecomendadas.join('\n'));
    }}
  />
)}
```

---

## 🎨 UI/UX ESPERADA

### Estado Inicial
- Botão discreto: "✨ Gerar Diagnóstico com IA" (gradient purple-blue)
- Só aparece quando problema tem 20+ caracteres

### Estado Loading
- Botão desabilitado com spinner
- Texto: "🤖 Analisando com IA..."
- Skeleton placeholders abaixo

### Estado Sucesso
Card com:
1. **Badge no topo:**
    - 🚀 "Diagnóstico gerado" (verde) OU
    - ♻️ "Diagnóstico similar encontrado" (azul) se veio do cache OU
    - 📝 "Diagnóstico padrão" (amarelo) se veio de template

2. **Resumo:** Texto limpo com o overview

3. **Causas Possíveis:**
    - Lista com badges de probabilidade (%)
    - Badges de gravidade coloridos (ALTA=vermelho, MÉDIA=amarelo, BAIXA=verde)

4. **Ações Recomendadas:** Lista de bullet points

5. **Peças Prováveis:** Cards com nome + badges de urgência

6. **Estimativas:** Grid 2 colunas (tempo | custo)

7. **Botão de Ação:** "✓ Aceitar Sugestão" (preenche campos automaticamente)

8. **Disclaimer:** Texto pequeno: "💡 Diagnóstico assistido por IA. Validação profissional recomendada."

---

## ✅ CRITÉRIOS DE ACEITAÇÃO

### Backend:
- [ ] Endpoint POST /api/v1/diagnostico-ia funcional
- [ ] Cache Redis implementado (verificar no Redis CLI)
- [ ] Pré-validação funcional (logs mostram "template usado")
- [ ] Model routing funcional (70% Haiku, 30% Sonnet nos logs)
- [ ] Context compression funcional (histórico <= 500 tokens)
- [ ] Métricas registradas (tokens consumidos, custo estimado)
- [ ] Testes unitários para cada service
- [ ] Tratamento de erros (API offline, token inválido, etc)

### Frontend:
- [ ] Botão aparece só quando apropriado
- [ ] Loading state fluido
- [ ] Resultado visualmente agradável
- [ ] Badges de probabilidade/gravidade/urgência
- [ ] Botão "Aceitar Sugestão" preenche campos
- [ ] Tratamento de erro (toast notification)
- [ ] Responsivo (mobile friendly)

### Performance:
- [ ] Resposta < 3 segundos (95th percentile)
- [ ] Cache hit rate > 25% após 1 semana
- [ ] Custo médio < $0.05 por diagnóstico
- [ ] Template usage > 20% das requisições

---

## 🔐 SEGURANÇA

- [ ] API key em variável de ambiente (nunca no código)
- [ ] Endpoint protegido (autenticação JWT)
- [ ] Rate limiting (10 diagnósticos/minuto por usuário)
- [ ] Input sanitization (remover caracteres perigosos)
- [ ] Logs não expõem dados sensíveis

---

## 📊 OBSERVABILIDADE

Adicione logs estruturados em pontos críticos:
```java
log.info("Diagnóstico solicitado - veiculoId={}, tokens={}, modelo={}", 
         veiculoId, tokensUsados, modelo);

log.info("Cache hit - economia=${}", custoEconomizado);

log.warn("Pré-validação falhou - problema não comum");
```

---

## 📝 ENTREGÁVEIS

1. **Backend:**
    - Services criados e testados
    - Controller com endpoint
    - Models/DTOs
    - Config do Anthropic
    - Migrations (se necessário)

2. **Frontend:**
    - Componente DiagnosticoIA.tsx
    - Service de API
    - Integração na tela de Nova OS
    - Types/Interfaces TypeScript

3. **Documentação:**
    - README com setup da API key
    - Exemplos de uso
    - Métricas esperadas

---

## 🚀 PRÓXIMOS PASSOS APÓS IMPLEMENTAÇÃO

1. Validar custo real vs estimado
2. Ajustar thresholds de cache
3. Treinar templates com dados reais
4. A/B test: com IA vs sem IA (taxa de aprovação)
5. Feedback loop: mecânico valida diagnóstico

---

## ⚠️ ATENÇÃO

- **NÃO** envie histórico completo para IA (caro e lento)
- **NÃO** use JSON complexo se pipe-separated funciona
- **SEMPRE** cache quando possível
- **PREFIRA** modelo barato quando suficiente
- **VALIDE** input antes de chamar API
- **MONITORE** custos em tempo real

---

## 🎯 RESULTADO ESPERADO

Ao final, a tela de Nova Ordem de Serviço deve ter um botão mágico que:
1. Analisa o problema em 2-3 segundos
2. Sugere causas prováveis com probabilidades
3. Recomenda ações e peças
4. Economiza 70-80% em custos de API
5. Melhora produtividade do mecânico em 40%

**Custo alvo:** $0.02 - $0.05 por diagnóstico (vs $0.15 - $0.25 sem otimização)