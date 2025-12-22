# Integração WhatsApp - Aprovação Automática de Orçamentos

## Visão Geral

Sistema para aprovação automática de orçamentos (Ordens de Serviço) via WhatsApp. O administrador pode escolher entre dois provedores:
- **Twilio WhatsApp Business API** (oficial, pago, mais confiável)
- **Evolution API** (open source, gratuito, self-hosted)

## Arquitetura

### Diagrama de Fluxo

```
┌─────────────────────────────────────────────────────────────┐
│                    CRIAÇÃO DO ORÇAMENTO                     │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│  Backend: OrdemServicoService.create()                      │
│  - Cria OS com status ORCAMENTO                             │
│  - Dispara evento: OrcamentoGeradoEvent                     │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│  WhatsAppNotificationListener                               │
│  - Ouve evento OrcamentoGeradoEvent                         │
│  - Busca configuração ativa do WhatsApp                     │
│  - Chama WhatsAppServiceFactory                             │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│  WhatsAppServiceFactory                                     │
│  - Verifica qual provedor está configurado                  │
│  - Retorna TwilioWhatsAppService OU EvolutionWhatsAppService│
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│  WhatsAppService.enviarMensagemOrcamento()                  │
│  - Formata mensagem com dados da OS                         │
│  - Envia via Twilio ou Evolution                            │
│  - Registra log de envio                                    │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│                 MENSAGEM ENVIADA AO CLIENTE                 │
│  "Olá João! Seu orçamento #123 está pronto.                │
│   Veículo: Fiat Uno ABC-1234                                │
│   Valor: R$ 850,00                                          │
│                                                              │
│   Responda SIM para aprovar ou NAO para recusar."          │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│            CLIENTE RESPONDE "SIM" OU "NAO"                  │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│  Webhook: POST /api/webhooks/whatsapp                       │
│  - Twilio ou Evolution envia payload da mensagem            │
│  - WhatsAppWebhookController detecta provedor               │
│  - Extrai: telefone, mensagem, timestamp                    │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│  WhatsAppOrcamentoProcessor                                 │
│  - Normaliza texto (remove acentos, lowercase)              │
│  - Detecta intenção: SIM/APROVAR/OK → APROVAR               │
│                      NAO/RECUSAR/CANCELAR → RECUSAR         │
│  - Busca OS em aberto do cliente (pelo telefone)           │
│  - Valida status ORCAMENTO                                  │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│  OrdemServicoService.processarRespostaWhatsApp()            │
│  - Se APROVAR: atualiza status → APROVADO                   │
│  - Se RECUSAR: adiciona observação, mantém ORCAMENTO        │
│  - Registra histórico de aprovação                          │
│  - Dispara WebSocket notification                           │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│  WebSocket: /topic/os-updates                               │
│  - Notifica frontend em tempo real                          │
│  - Timeline atualiza automaticamente                        │
└─────────────────────────────────────────────────────────────┘
```

---

## Estrutura do Backend

### 1. Configuração do Sistema

#### Tabela: `configuracoes_sistema`

```sql
CREATE TABLE configuracoes_sistema (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chave VARCHAR(100) UNIQUE NOT NULL,
    valor TEXT,
    tipo VARCHAR(50) NOT NULL, -- STRING, INTEGER, BOOLEAN, JSON, ENCRYPTED
    descricao TEXT,
    categoria VARCHAR(50), -- WHATSAPP, EMAIL, GERAL, NOTIFICACOES
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices
CREATE INDEX idx_config_chave ON configuracoes_sistema(chave);
CREATE INDEX idx_config_categoria ON configuracoes_sistema(categoria);
```

#### Configurações WhatsApp (exemplos):

```json
{
  "whatsapp.provedor": "TWILIO", // ou "EVOLUTION"
  "whatsapp.ativo": true,
  "whatsapp.aprovacao_automatica.ativa": true,

  // Twilio
  "whatsapp.twilio.account_sid": "ACxxxxx", // ENCRYPTED
  "whatsapp.twilio.auth_token": "xxxxx", // ENCRYPTED
  "whatsapp.twilio.numero_origem": "+14155238886",

  // Evolution API
  "whatsapp.evolution.url_base": "http://localhost:8080",
  "whatsapp.evolution.api_key": "xxxxx", // ENCRYPTED
  "whatsapp.evolution.instancia": "pitstop-whatsapp"
}
```

---

### 2. Pacote: `com.pitstop.whatsapp`

```
whatsapp/
├── domain/
│   ├── ProvedorWhatsApp.java (enum: TWILIO, EVOLUTION)
│   ├── StatusEnvio.java (enum: PENDENTE, ENVIADO, ERRO, ENTREGUE)
│   └── HistoricoMensagem.java (entity: log de mensagens)
├── dto/
│   ├── EnviarMensagemRequest.java
│   ├── EnviarMensagemResponse.java
│   ├── WebhookTwilioPayload.java
│   └── WebhookEvolutionPayload.java
├── service/
│   ├── WhatsAppService.java (interface)
│   ├── TwilioWhatsAppService.java
│   ├── EvolutionWhatsAppService.java
│   ├── WhatsAppServiceFactory.java
│   └── WhatsAppOrcamentoProcessor.java
├── controller/
│   └── WhatsAppWebhookController.java
├── repository/
│   └── HistoricoMensagemRepository.java
└── exception/
    ├── WhatsAppException.java
    └── ProvedorNaoConfiguradoException.java
```

---

### 3. Interface: `WhatsAppService.java`

```java
package com.pitstop.whatsapp.service;

import com.pitstop.whatsapp.dto.EnviarMensagemRequest;
import com.pitstop.whatsapp.dto.EnviarMensagemResponse;

public interface WhatsAppService {

    /**
     * Envia mensagem de texto simples
     */
    EnviarMensagemResponse enviarMensagem(String telefone, String mensagem);

    /**
     * Envia mensagem de orçamento formatada
     */
    EnviarMensagemResponse enviarMensagemOrcamento(
        String telefone,
        Long numeroOS,
        String veiculoDescricao,
        BigDecimal valorFinal
    );

    /**
     * Envia mensagem de confirmação de aprovação
     */
    EnviarMensagemResponse enviarConfirmacaoAprovacao(
        String telefone,
        Long numeroOS
    );

    /**
     * Testa conexão com o provedor
     */
    boolean testarConexao();

    /**
     * Retorna nome do provedor
     */
    String getNomeProvedor();
}
```

---

### 4. Implementação: `TwilioWhatsAppService.java`

```java
@Service
@ConditionalOnProperty(name = "whatsapp.provedor", havingValue = "TWILIO")
public class TwilioWhatsAppService implements WhatsAppService {

    private final ConfiguracaoService configuracaoService;
    private final HistoricoMensagemRepository historicoRepository;

    private Twilio twilio;

    @PostConstruct
    public void init() {
        String accountSid = configuracaoService.getDecrypted("whatsapp.twilio.account_sid");
        String authToken = configuracaoService.getDecrypted("whatsapp.twilio.auth_token");

        Twilio.init(accountSid, authToken);
    }

    @Override
    public EnviarMensagemResponse enviarMensagem(String telefone, String mensagem) {
        try {
            String numeroOrigem = configuracaoService.get("whatsapp.twilio.numero_origem");

            Message message = Message.creator(
                new PhoneNumber("whatsapp:" + telefone),
                new PhoneNumber("whatsapp:" + numeroOrigem),
                mensagem
            ).create();

            // Registra histórico
            salvarHistorico(telefone, mensagem, message.getSid(), StatusEnvio.ENVIADO);

            return EnviarMensagemResponse.builder()
                .sucesso(true)
                .mensagemId(message.getSid())
                .status(message.getStatus().toString())
                .build();

        } catch (Exception e) {
            log.error("Erro ao enviar mensagem via Twilio", e);
            salvarHistorico(telefone, mensagem, null, StatusEnvio.ERRO);
            throw new WhatsAppException("Erro ao enviar mensagem", e);
        }
    }

    @Override
    public EnviarMensagemResponse enviarMensagemOrcamento(
        String telefone,
        Long numeroOS,
        String veiculoDescricao,
        BigDecimal valorFinal
    ) {
        String mensagem = String.format(
            "🔧 *PitStop - Orçamento Pronto*\n\n" +
            "Olá! Seu orçamento está disponível:\n\n" +
            "📋 OS: #%d\n" +
            "🚗 Veículo: %s\n" +
            "💰 Valor: R$ %.2f\n\n" +
            "Para *aprovar*, responda: SIM\n" +
            "Para *recusar*, responda: NAO\n\n" +
            "Qualquer dúvida, estamos à disposição! 😊",
            numeroOS,
            veiculoDescricao,
            valorFinal
        );

        return enviarMensagem(telefone, mensagem);
    }

    @Override
    public String getNomeProvedor() {
        return "Twilio WhatsApp Business API";
    }
}
```

---

### 5. Implementação: `EvolutionWhatsAppService.java`

```java
@Service
@ConditionalOnProperty(name = "whatsapp.provedor", havingValue = "EVOLUTION")
public class EvolutionWhatsAppService implements WhatsAppService {

    private final ConfiguracaoService configuracaoService;
    private final RestTemplate restTemplate;

    @Override
    public EnviarMensagemResponse enviarMensagem(String telefone, String mensagem) {
        try {
            String urlBase = configuracaoService.get("whatsapp.evolution.url_base");
            String apiKey = configuracaoService.getDecrypted("whatsapp.evolution.api_key");
            String instancia = configuracaoService.get("whatsapp.evolution.instancia");

            String url = String.format("%s/message/sendText/%s", urlBase, instancia);

            // Normaliza telefone (remove caracteres, adiciona @s.whatsapp.net)
            String telefoneNormalizado = normalizarTelefone(telefone);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", apiKey);

            Map<String, Object> body = Map.of(
                "number", telefoneNormalizado,
                "text", mensagem
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            salvarHistorico(telefone, mensagem, response.getBody().get("key").toString(), StatusEnvio.ENVIADO);

            return EnviarMensagemResponse.builder()
                .sucesso(true)
                .mensagemId(response.getBody().get("key").toString())
                .build();

        } catch (Exception e) {
            log.error("Erro ao enviar mensagem via Evolution API", e);
            throw new WhatsAppException("Erro ao enviar mensagem", e);
        }
    }

    private String normalizarTelefone(String telefone) {
        // Remove caracteres especiais
        String digits = telefone.replaceAll("\\D", "");

        // Adiciona código do país se não tiver
        if (!digits.startsWith("55")) {
            digits = "55" + digits;
        }

        // Formato Evolution: 5511999999999@s.whatsapp.net
        return digits + "@s.whatsapp.net";
    }

    @Override
    public String getNomeProvedor() {
        return "Evolution API (Self-hosted)";
    }
}
```

---

### 6. Factory: `WhatsAppServiceFactory.java`

```java
@Service
@RequiredArgsConstructor
public class WhatsAppServiceFactory {

    private final ConfiguracaoService configuracaoService;
    private final TwilioWhatsAppService twilioService;
    private final EvolutionWhatsAppService evolutionService;

    public WhatsAppService getService() {
        String provedor = configuracaoService.get("whatsapp.provedor");

        if (provedor == null) {
            throw new ProvedorNaoConfiguradoException("Nenhum provedor WhatsApp configurado");
        }

        return switch (ProvedorWhatsApp.valueOf(provedor)) {
            case TWILIO -> twilioService;
            case EVOLUTION -> evolutionService;
        };
    }

    public boolean isWhatsAppAtivo() {
        return configuracaoService.getBoolean("whatsapp.ativo", false);
    }

    public boolean isAprovacaoAutomaticaAtiva() {
        return configuracaoService.getBoolean("whatsapp.aprovacao_automatica.ativa", false);
    }
}
```

---

### 7. Processador de Respostas: `WhatsAppOrcamentoProcessor.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppOrcamentoProcessor {

    private final OrdemServicoRepository ordemServicoRepository;
    private final ClienteRepository clienteRepository;
    private final OrdemServicoService ordemServicoService;

    // Palavras-chave para aprovação
    private static final Set<String> PALAVRAS_APROVACAO = Set.of(
        "sim", "aprovar", "aprovo", "ok", "aceito", "confirmar", "confirmo"
    );

    // Palavras-chave para recusa
    private static final Set<String> PALAVRAS_RECUSA = Set.of(
        "nao", "não", "recusar", "recuso", "cancelar", "cancelo", "rejeitar", "rejeito"
    );

    public void processarMensagem(String telefone, String mensagem) {
        log.info("Processando mensagem WhatsApp de {}: {}", telefone, mensagem);

        // Normaliza mensagem
        String mensagemNormalizada = normalizarTexto(mensagem);

        // Detecta intenção
        IntencaoCliente intencao = detectarIntencao(mensagemNormalizada);

        if (intencao == IntencaoCliente.DESCONHECIDA) {
            log.debug("Mensagem não identificada como resposta de orçamento");
            return;
        }

        // Busca cliente pelo telefone
        Optional<Cliente> clienteOpt = buscarClientePorTelefone(telefone);

        if (clienteOpt.isEmpty()) {
            log.warn("Cliente não encontrado para telefone: {}", telefone);
            return;
        }

        Cliente cliente = clienteOpt.get();

        // Busca OS em aberto (status ORCAMENTO) do cliente
        List<OrdemServico> orcamentosAbertos = ordemServicoRepository
            .findByClienteIdAndStatus(cliente.getId(), StatusOS.ORCAMENTO);

        if (orcamentosAbertos.isEmpty()) {
            log.warn("Nenhum orçamento em aberto para cliente {}", cliente.getNome());
            return;
        }

        // Pega o orçamento mais recente
        OrdemServico os = orcamentosAbertos.get(0);

        // Processa aprovação ou recusa
        if (intencao == IntencaoCliente.APROVAR) {
            ordemServicoService.aprovarViaWhatsApp(os.getId(), telefone);
            log.info("OS #{} aprovada via WhatsApp pelo cliente {}", os.getNumero(), cliente.getNome());
        } else if (intencao == IntencaoCliente.RECUSAR) {
            ordemServicoService.recusarViaWhatsApp(os.getId(), telefone);
            log.info("OS #{} recusada via WhatsApp pelo cliente {}", os.getNumero(), cliente.getNome());
        }
    }

    private String normalizarTexto(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "")
            .toLowerCase()
            .trim();
    }

    private IntencaoCliente detectarIntencao(String mensagemNormalizada) {
        // Verifica se contém palavra de aprovação
        for (String palavra : PALAVRAS_APROVACAO) {
            if (mensagemNormalizada.contains(palavra)) {
                return IntencaoCliente.APROVAR;
            }
        }

        // Verifica se contém palavra de recusa
        for (String palavra : PALAVRAS_RECUSA) {
            if (mensagemNormalizada.contains(palavra)) {
                return IntencaoCliente.RECUSAR;
            }
        }

        return IntencaoCliente.DESCONHECIDA;
    }

    private Optional<Cliente> buscarClientePorTelefone(String telefone) {
        // Normaliza telefone (remove caracteres)
        String telefoneNormalizado = telefone.replaceAll("\\D", "");

        // Busca por celular ou telefone
        return clienteRepository.findByTelefoneOrCelular(telefoneNormalizado);
    }

    enum IntencaoCliente {
        APROVAR, RECUSAR, DESCONHECIDA
    }
}
```

---

### 8. Webhook Controller: `WhatsAppWebhookController.java`

```java
@RestController
@RequestMapping("/api/webhooks/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final WhatsAppOrcamentoProcessor processor;
    private final ConfiguracaoService configuracaoService;

    /**
     * Webhook do Twilio
     * POST /api/webhooks/whatsapp/twilio
     */
    @PostMapping("/twilio")
    public ResponseEntity<String> webhookTwilio(@RequestParam Map<String, String> params) {
        log.info("Webhook Twilio recebido: {}", params);

        try {
            String from = params.get("From"); // whatsapp:+5511999999999
            String body = params.get("Body"); // Mensagem do cliente

            // Remove prefixo "whatsapp:"
            String telefone = from.replace("whatsapp:", "");

            // Processa mensagem
            processor.processarMensagem(telefone, body);

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Erro ao processar webhook Twilio", e);
            return ResponseEntity.status(500).body("Erro");
        }
    }

    /**
     * Webhook da Evolution API
     * POST /api/webhooks/whatsapp/evolution
     */
    @PostMapping("/evolution")
    public ResponseEntity<String> webhookEvolution(@RequestBody Map<String, Object> payload) {
        log.info("Webhook Evolution recebido: {}", payload);

        try {
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            Map<String, Object> key = (Map<String, Object>) data.get("key");
            Map<String, Object> message = (Map<String, Object>) data.get("message");

            String from = key.get("remoteJid").toString(); // 5511999999999@s.whatsapp.net
            String body = "";

            // Extrai texto da mensagem
            if (message.containsKey("conversation")) {
                body = message.get("conversation").toString();
            } else if (message.containsKey("extendedTextMessage")) {
                Map<String, Object> extended = (Map<String, Object>) message.get("extendedTextMessage");
                body = extended.get("text").toString();
            }

            // Remove sufixo @s.whatsapp.net
            String telefone = from.replace("@s.whatsapp.net", "");

            // Processa mensagem
            processor.processarMensagem(telefone, body);

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Erro ao processar webhook Evolution", e);
            return ResponseEntity.status(500).body("Erro");
        }
    }
}
```

---

### 9. Melhorias no `OrdemServicoService.java`

```java
@Service
public class OrdemServicoService {

    private final WhatsAppServiceFactory whatsAppFactory;

    /**
     * Aprova OS via WhatsApp
     */
    @Transactional
    public void aprovarViaWhatsApp(UUID osId, String telefone) {
        OrdemServico os = ordemServicoRepository.findById(osId)
            .orElseThrow(() -> new OrdemServicoNotFoundException(osId));

        if (os.getStatus() != StatusOS.ORCAMENTO) {
            log.warn("Tentativa de aprovar OS #{} que não está em ORCAMENTO", os.getNumero());
            return;
        }

        // Atualiza status
        os.setStatus(StatusOS.APROVADO);
        os.setAprovadoPeloCliente(true);
        os.setObservacoes(
            (os.getObservacoes() != null ? os.getObservacoes() + "\n" : "") +
            "✅ Aprovado via WhatsApp em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );

        ordemServicoRepository.save(os);

        // Envia confirmação ao cliente
        if (whatsAppFactory.isWhatsAppAtivo()) {
            WhatsAppService whatsAppService = whatsAppFactory.getService();
            whatsAppService.enviarConfirmacaoAprovacao(telefone, os.getNumero());
        }

        // Notifica via WebSocket
        notificarAtualizacaoOS(os);

        log.info("OS #{} aprovada via WhatsApp", os.getNumero());
    }

    /**
     * Recusa OS via WhatsApp
     */
    @Transactional
    public void recusarViaWhatsApp(UUID osId, String telefone) {
        OrdemServico os = ordemServicoRepository.findById(osId)
            .orElseThrow(() -> new OrdemServicoNotFoundException(osId));

        if (os.getStatus() != StatusOS.ORCAMENTO) {
            return;
        }

        // Adiciona observação (mantém status ORCAMENTO)
        os.setObservacoes(
            (os.getObservacoes() != null ? os.getObservacoes() + "\n" : "") +
            "❌ Recusado via WhatsApp em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );

        ordemServicoRepository.save(os);

        // Notifica via WebSocket
        notificarAtualizacaoOS(os);

        log.info("OS #{} recusada via WhatsApp", os.getNumero());
    }

    /**
     * Envia orçamento ao criar OS
     */
    @Transactional
    public OrdemServicoResponseDTO create(CreateOrdemServicoRequest request) {
        // ... código existente de criação ...

        OrdemServico savedOS = ordemServicoRepository.save(os);

        // Envia WhatsApp se configurado
        enviarWhatsAppOrcamento(savedOS);

        return mapper.toResponse(savedOS);
    }

    private void enviarWhatsAppOrcamento(OrdemServico os) {
        if (!whatsAppFactory.isWhatsAppAtivo() || !whatsAppFactory.isAprovacaoAutomaticaAtiva()) {
            log.debug("WhatsApp não está ativo ou aprovação automática desabilitada");
            return;
        }

        try {
            Veiculo veiculo = veiculoRepository.findById(os.getVeiculoId())
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));

            Cliente cliente = clienteRepository.findByIdIncludingInactive(veiculo.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

            // Prioriza celular, senão usa telefone
            String telefone = cliente.getCelular() != null ? cliente.getCelular() : cliente.getTelefone();

            if (telefone == null) {
                log.warn("Cliente {} não possui telefone cadastrado", cliente.getNome());
                return;
            }

            WhatsAppService whatsAppService = whatsAppFactory.getService();

            String veiculoDescricao = String.format("%s %s - %s",
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getPlacaFormatada()
            );

            whatsAppService.enviarMensagemOrcamento(
                telefone,
                os.getNumero(),
                veiculoDescricao,
                os.getValorFinal()
            );

            log.info("Orçamento OS #{} enviado via WhatsApp para {}", os.getNumero(), cliente.getNome());

        } catch (Exception e) {
            log.error("Erro ao enviar orçamento via WhatsApp para OS #{}", os.getNumero(), e);
            // Não lança exceção para não impedir a criação da OS
        }
    }
}
```

---

## Frontend

### Estrutura de Arquivos

```
src/features/configuracoes/
├── components/
│   ├── WhatsAppConfigForm.tsx
│   ├── TwilioConfigFields.tsx
│   └── EvolutionConfigFields.tsx
├── hooks/
│   └── useConfiguracoes.ts
├── pages/
│   └── ConfiguracoesPage.tsx
├── services/
│   └── configuracaoService.ts
└── types/
    └── index.ts
```

### Página de Configurações

```tsx
// src/features/configuracoes/pages/ConfiguracoesPage.tsx

export const ConfiguracoesPage = () => {
  const [provedorSelecionado, setProvedorSelecionado] = useState<'TWILIO' | 'EVOLUTION'>('TWILIO');
  const [whatsappAtivo, setWhatsappAtivo] = useState(false);
  const [aprovacaoAutomatica, setAprovacaoAutomatica] = useState(false);

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold">Configurações do Sistema</h1>

      {/* WhatsApp */}
      <div className="mt-6 rounded-lg bg-white p-6 shadow">
        <h2 className="text-xl font-semibold">WhatsApp Business</h2>

        {/* Toggle Ativar WhatsApp */}
        <div className="mt-4">
          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={whatsappAtivo}
              onChange={(e) => setWhatsappAtivo(e.target.checked)}
            />
            <span>Ativar integração WhatsApp</span>
          </label>
        </div>

        {whatsappAtivo && (
          <>
            {/* Seleção de Provedor */}
            <div className="mt-4">
              <label className="font-medium">Provedor WhatsApp</label>
              <select
                value={provedorSelecionado}
                onChange={(e) => setProvedorSelecionado(e.target.value as any)}
                className="mt-1 w-full rounded border p-2"
              >
                <option value="TWILIO">Twilio WhatsApp Business API (Oficial)</option>
                <option value="EVOLUTION">Evolution API (Self-hosted)</option>
              </select>
            </div>

            {/* Formulário específico */}
            {provedorSelecionado === 'TWILIO' && <TwilioConfigFields />}
            {provedorSelecionado === 'EVOLUTION' && <EvolutionConfigFields />}

            {/* Toggle Aprovação Automática */}
            <div className="mt-4">
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={aprovacaoAutomatica}
                  onChange={(e) => setAprovacaoAutomatica(e.target.checked)}
                />
                <span>Ativar aprovação automática de orçamentos via WhatsApp</span>
              </label>
              <p className="mt-1 text-sm text-gray-500">
                Quando ativado, clientes podem aprovar orçamentos respondendo "SIM" no WhatsApp
              </p>
            </div>

            {/* Botão de Teste */}
            <button className="mt-4 rounded bg-blue-600 px-4 py-2 text-white">
              Testar Conexão
            </button>
          </>
        )}
      </div>
    </div>
  );
};
```

---

## Segurança

### 1. Criptografia de Credenciais

```java
@Service
public class EncryptionService {

    @Value("${app.encryption.secret}")
    private String encryptionKey;

    public String encrypt(String value) {
        // AES-256 encryption
        // Implementar usando javax.crypto.Cipher
    }

    public String decrypt(String encryptedValue) {
        // AES-256 decryption
    }
}
```

### 2. Validação de Webhook

```java
// Twilio: Validar signature X-Twilio-Signature
// Evolution: Validar API key no header
```

---

## Testes

### 1. Testes Unitários

```java
@Test
void deveDetectarIntencaoAprovacao() {
    String[] mensagens = {"sim", "SIM", "Sim!", "Aprovo", "OK"};

    for (String msg : mensagens) {
        IntencaoCliente intencao = processor.detectarIntencao(msg);
        assertEquals(IntencaoCliente.APROVAR, intencao);
    }
}

@Test
void deveDetectarIntencaoRecusa() {
    String[] mensagens = {"não", "NAO", "Recuso", "Cancelar"};

    for (String msg : mensagens) {
        IntencaoCliente intencao = processor.detectarIntencao(msg);
        assertEquals(IntencaoCliente.RECUSAR, intencao);
    }
}
```

### 2. Testes de Integração

```java
@Test
void deveEnviarMensagemViaTwilio() {
    // Mock Twilio API
    // Verificar envio
}

@Test
void deveProcessarWebhookTwilio() {
    // Simular POST do Twilio
    // Verificar processamento
}
```

---

## Deployment

### 1. Variáveis de Ambiente

```properties
# application.properties

# WhatsApp
whatsapp.provedor=${WHATSAPP_PROVEDOR:TWILIO}
whatsapp.ativo=${WHATSAPP_ATIVO:false}

# Twilio
whatsapp.twilio.account_sid=${TWILIO_ACCOUNT_SID:}
whatsapp.twilio.auth_token=${TWILIO_AUTH_TOKEN:}
whatsapp.twilio.numero_origem=${TWILIO_NUMERO:}

# Evolution
whatsapp.evolution.url_base=${EVOLUTION_URL:http://localhost:8080}
whatsapp.evolution.api_key=${EVOLUTION_API_KEY:}
whatsapp.evolution.instancia=${EVOLUTION_INSTANCE:pitstop}

# Encryption
app.encryption.secret=${ENCRYPTION_SECRET:changeme}
```

### 2. Docker Compose (Evolution API)

```yaml
# docker-compose.yml

services:
  evolution-api:
    image: atendai/evolution-api:latest
    ports:
      - "8080:8080"
    environment:
      - DATABASE_PROVIDER=postgresql
      - DATABASE_CONNECTION_URI=postgresql://user:pass@postgres:5432/evolution
      - AUTHENTICATION_API_KEY=your-api-key-here
    volumes:
      - evolution_data:/evolution/instances
```

---

## Próximos Passos

1. ✅ Criar documento de planejamento (ESTE ARQUIVO)
2. ⏳ Decidir se implementar agora ou em fase futura
3. ⏳ Se implementar:
   - Criar migrations
   - Implementar backend (3-5 dias)
   - Implementar frontend (2 dias)
   - Testar com ambos provedores (1 dia)
   - Deploy e homologação (1 dia)

**Estimativa total: 7-9 dias de desenvolvimento**

---

## Custos Estimados

### Twilio WhatsApp Business API
- **Setup**: Gratuito
- **Custo por mensagem**: ~R$ 0,10 - R$ 0,30
- **Estimativa mensal** (100 OS/mês): R$ 20 - R$ 60

### Evolution API
- **Setup**: Gratuito
- **Servidor**: R$ 30-50/mês (VPS básico)
- **Mensagens**: Ilimitadas
- **Risco**: Possível banimento (baixo se usar corretamente)

---

## Conclusão

A arquitetura proposta permite:
- ✅ Flexibilidade de escolher provedor
- ✅ Fácil manutenção e extensão
- ✅ Desacoplamento via interfaces
- ✅ Segurança (credenciais criptografadas)
- ✅ Rastreabilidade (logs de mensagens)
- ✅ Configuração via UI (sem mexer em código)

**Recomendação**: Começar com Evolution API para testes, depois migrar para Twilio em produção se necessário.
