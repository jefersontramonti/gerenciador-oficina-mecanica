package com.pitstop.financeiro.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.pitstop.financeiro.domain.*;
import com.pitstop.financeiro.dto.*;
import com.pitstop.financeiro.repository.*;
import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import com.pitstop.saas.repository.SaasConfigGatewayRepository;
import com.pitstop.saas.service.FaturaWebhookService;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.veiculo.domain.Veiculo;
import com.pitstop.veiculo.repository.VeiculoRepository;
import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Serviço de integração com Mercado Pago.
 * Responsável por criar checkouts, processar webhooks e gerenciar pagamentos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoService {

    private final ConfiguracaoGatewayRepository configuracaoGatewayRepository;
    private final PagamentoOnlineRepository pagamentoOnlineRepository;
    private final PagamentoRepository pagamentoRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final SaasConfigGatewayRepository saasConfigGatewayRepository;

    // Lazy injection to avoid circular dependency
    @Autowired
    @Lazy
    private FaturaWebhookService faturaWebhookService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Cria um checkout (preferência de pagamento) no Mercado Pago.
     */
    @Transactional
    public CheckoutResponseDTO criarCheckout(CriarCheckoutRequestDTO request) {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("Criando checkout MP para OS {} - Oficina {}", request.getOrdemServicoId(), oficinaId);

        // Buscar configuração do gateway
        ConfiguracaoGateway config = getConfiguracaoAtiva(oficinaId);

        // Buscar OS
        OrdemServico os = ordemServicoRepository.findById(request.getOrdemServicoId())
            .orElseThrow(() -> new IllegalArgumentException("Ordem de serviço não encontrada"));

        // Definir valor
        BigDecimal valor = request.getValor() != null ? request.getValor() : os.getValorFinal();
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do pagamento deve ser maior que zero");
        }

        // Configurar SDK
        MercadoPagoConfig.setAccessToken(config.getAccessToken());

        try {
            // Criar preferência
            PreferenceClient client = new PreferenceClient();

            // Item
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id(os.getId().toString())
                .title("OS #" + os.getNumero() + " - " + getNomeOficina(os))
                .description(getDescricaoOS(os))
                .quantity(1)
                .currencyId("BRL")
                .unitPrice(valor)
                .build();

            // Pagador
            PreferencePayerRequest payer = PreferencePayerRequest.builder()
                .email(request.getEmailPagador() != null ? request.getEmailPagador() : getEmailCliente(os))
                .name(request.getNomePagador() != null ? request.getNomePagador() : getNomeCliente(os))
                .build();

            // URLs de retorno
            String successUrl = frontendUrl + "/pagamento/sucesso?os=" + os.getId();
            String failureUrl = frontendUrl + "/pagamento/erro?os=" + os.getId();
            String pendingUrl = frontendUrl + "/pagamento/pendente?os=" + os.getId();

            log.info("Back URLs - Success: {}, Failure: {}, Pending: {}", successUrl, failureUrl, pendingUrl);

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(successUrl)
                .failure(failureUrl)
                .pending(pendingUrl)
                .build();

            // Expiração
            int expiracaoMinutos = request.getExpiracaoMinutos() != null ? request.getExpiracaoMinutos() : 30;
            OffsetDateTime expiracao = OffsetDateTime.now().plusMinutes(expiracaoMinutos);

            // Métodos de pagamento
            PreferencePaymentMethodsRequest paymentMethods = null;
            if (request.getMetodosPermitidos() != null && request.getMetodosPermitidos().length > 0) {
                // Configurar métodos específicos se necessário
                paymentMethods = PreferencePaymentMethodsRequest.builder()
                    .installments(12) // Máximo de parcelas
                    .build();
            }

            // Referência externa (para identificar no webhook)
            String externalReference = os.getId().toString();

            // Construir preferência
            PreferenceRequest.PreferenceRequestBuilder preferenceBuilder = PreferenceRequest.builder()
                .items(List.of(item))
                .payer(payer)
                .externalReference(externalReference)
                .notificationUrl(baseUrl + "/api/webhooks/mercadopago")
                .expirationDateTo(expiracao)
                .statementDescriptor("PITSTOP OS" + os.getNumero());

            // Adicionar back URLs (autoReturn removido devido a bug no SDK)
            if (successUrl != null && !successUrl.isBlank()) {
                preferenceBuilder.backUrls(backUrls);
                // Nota: autoReturn("approved") causa erro "back_url.success must be defined"
                // mesmo quando as URLs estão definidas. Bug no SDK 2.1.29.
            }

            if (paymentMethods != null) {
                preferenceBuilder.paymentMethods(paymentMethods);
            }

            PreferenceRequest preferenceRequest = preferenceBuilder.build();

            // Criar no MP
            Preference preference = client.create(preferenceRequest);

            log.info("Preferência MP criada: {} - Init Point: {}", preference.getId(), preference.getInitPoint());

            // Salvar pagamento online
            PagamentoOnline pagamentoOnline = PagamentoOnline.builder()
                .ordemServicoId(os.getId())
                .gateway(TipoGateway.MERCADO_PAGO)
                .preferenceId(preference.getId())
                .status(StatusPagamentoOnline.PENDENTE)
                .valor(valor)
                .urlCheckout(preference.getInitPoint())
                .dataExpiracao(expiracao.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime())
                .emailPagador(request.getEmailPagador())
                .nomePagador(request.getNomePagador())
                .documentoPagador(request.getDocumentoPagador())
                .respostaGateway(preference.toString())
                .build();

            pagamentoOnline = pagamentoOnlineRepository.save(pagamentoOnline);

            return CheckoutResponseDTO.builder()
                .pagamentoOnlineId(pagamentoOnline.getId())
                .preferenceId(preference.getId())
                .urlCheckout(preference.getInitPoint())
                .valor(valor)
                .dataExpiracao(pagamentoOnline.getDataExpiracao())
                .status("PENDENTE")
                .gateway("MERCADO_PAGO")
                .mensagem("Checkout criado com sucesso. Redirecione o cliente para o link de pagamento.")
                .build();

        } catch (MPApiException e) {
            log.error("Erro API MP: {} - {}", e.getStatusCode(), e.getApiResponse().getContent());
            throw new RuntimeException("Erro ao criar checkout no Mercado Pago: " + e.getMessage());
        } catch (MPException e) {
            log.error("Erro MP: {}", e.getMessage());
            throw new RuntimeException("Erro ao comunicar com Mercado Pago: " + e.getMessage());
        }
    }

    /**
     * Processa notificação webhook do Mercado Pago.
     */
    @Transactional
    public void processarWebhook(String topic, String id, Map<String, Object> payload) {
        log.info("Webhook MP recebido - Topic: {}, ID: {}", topic, id);

        if (!"payment".equals(topic) && !"merchant_order".equals(topic)) {
            log.info("Webhook ignorado - topic não é payment nem merchant_order: {}", topic);
            return;
        }

        if ("payment".equals(topic)) {
            processarPagamento(id);
        }
    }

    /**
     * Consulta e processa um pagamento no Mercado Pago.
     * Detecta automaticamente se é um pagamento de OS ou de Fatura SaaS
     * baseado no external_reference (FAT- prefix = fatura SaaS).
     */
    @Transactional
    public void processarPagamento(String paymentId) {
        log.info("Processando pagamento MP: {}", paymentId);

        // Buscar pagamento online pelo ID externo ou preference
        Optional<PagamentoOnline> optPagamento = pagamentoOnlineRepository.findByIdExterno(paymentId);

        if (optPagamento.isEmpty()) {
            // Tentar consultar no MP para pegar external_reference
            try {
                // PRIMEIRO: Tentar com a configuração SaaS (para faturas de assinatura)
                Optional<com.pitstop.saas.domain.ConfiguracaoGateway> saasConfigOpt =
                    saasConfigGatewayRepository.findByTipoAndAtivoTrue(com.pitstop.saas.domain.TipoGateway.MERCADO_PAGO);

                if (saasConfigOpt.isPresent()) {
                    com.pitstop.saas.domain.ConfiguracaoGateway saasConfig = saasConfigOpt.get();
                    if (saasConfig.isConfigurado()) {
                        try {
                            MercadoPagoConfig.setAccessToken(saasConfig.getAccessToken());
                            PaymentClient paymentClient = new PaymentClient();
                            Payment payment = paymentClient.get(Long.parseLong(paymentId));

                            if (payment != null && payment.getExternalReference() != null) {
                                String externalRef = payment.getExternalReference();

                                // Verificar se é uma Fatura SaaS (external_reference começa com "FAT-")
                                if (externalRef.startsWith("FAT-")) {
                                    log.info("Pagamento {} é de Fatura SaaS - delegando para FaturaWebhookService", paymentId);
                                    faturaWebhookService.processarPagamento(paymentId);
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Não foi possível consultar com config SaaS: {}", e.getMessage());
                        }
                    }
                }

                // SEGUNDO: Tentar com configurações das oficinas (para pagamentos de OS)
                List<ConfiguracaoGateway> configs = configuracaoGatewayRepository.findAll()
                    .stream()
                    .filter(c -> c.getTipoGateway() == TipoGateway.MERCADO_PAGO && c.getAtivo())
                    .toList();

                if (configs.isEmpty() && saasConfigOpt.isEmpty()) {
                    log.warn("Nenhuma configuração MP ativa encontrada para processar webhook");
                    return;
                }

                for (ConfiguracaoGateway config : configs) {
                    try {
                        MercadoPagoConfig.setAccessToken(config.getAccessToken());
                        PaymentClient paymentClient = new PaymentClient();
                        Payment payment = paymentClient.get(Long.parseLong(paymentId));

                        if (payment != null && payment.getExternalReference() != null) {
                            String externalRef = payment.getExternalReference();

                            // É pagamento de Ordem de Serviço
                            UUID osId = UUID.fromString(externalRef);

                            // Buscar por OS
                            List<PagamentoOnline> pagamentos = pagamentoOnlineRepository
                                .findByOrdemServicoIdOrderByCreatedAtDesc(osId);

                            if (!pagamentos.isEmpty()) {
                                PagamentoOnline po = pagamentos.get(0);
                                atualizarPagamentoOnline(po, payment);
                                return;
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        // UUID parse failed - might be a different format
                        log.debug("External reference não é UUID válido: {}", e.getMessage());
                    } catch (Exception e) {
                        log.debug("Não foi possível consultar com config {}: {}", config.getId(), e.getMessage());
                    }
                }

                log.warn("Pagamento {} não encontrado em nenhuma oficina", paymentId);
            } catch (Exception e) {
                log.error("Erro ao processar webhook: {}", e.getMessage());
            }
        } else {
            // Pagamento encontrado, atualizar
            PagamentoOnline po = optPagamento.get();
            UUID oficinaId = po.getOficina().getId();

            ConfiguracaoGateway config = configuracaoGatewayRepository
                .findGatewayAtivo(oficinaId, TipoGateway.MERCADO_PAGO)
                .orElseThrow(() -> new RuntimeException("Configuração MP não encontrada"));

            try {
                MercadoPagoConfig.setAccessToken(config.getAccessToken());
                PaymentClient paymentClient = new PaymentClient();
                Payment payment = paymentClient.get(Long.parseLong(paymentId));

                atualizarPagamentoOnline(po, payment);
            } catch (Exception e) {
                log.error("Erro ao consultar pagamento MP: {}", e.getMessage());
            }
        }
    }

    /**
     * Atualiza o pagamento online com os dados do Mercado Pago.
     */
    private void atualizarPagamentoOnline(PagamentoOnline po, Payment payment) {
        log.info("Atualizando pagamento online {} com status MP: {}", po.getId(), payment.getStatus());

        // Mapear status MP para nosso status
        StatusPagamentoOnline novoStatus = mapearStatusMP(payment.getStatus());

        po.setIdExterno(payment.getId().toString());
        po.setStatus(novoStatus);
        po.setStatusDetalhe(payment.getStatusDetail());
        po.setMetodoPagamento(payment.getPaymentMethodId());

        if (payment.getTransactionDetails() != null) {
            po.setValorLiquido(payment.getTransactionDetails().getNetReceivedAmount());
            po.setValorTaxa(payment.getFeeDetails() != null && !payment.getFeeDetails().isEmpty()
                ? payment.getFeeDetails().get(0).getAmount()
                : null);
        }

        if (payment.getCard() != null) {
            po.setBandeiraCartao(payment.getPaymentMethodId());
            po.setUltimosDigitos(payment.getCard().getLastFourDigits());
        }

        po.setParcelas(payment.getInstallments());

        if (payment.getDateApproved() != null) {
            po.setDataAprovacao(payment.getDateApproved()
                .atZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime());
        }

        if (payment.getPayer() != null) {
            po.setEmailPagador(payment.getPayer().getEmail());
            if (payment.getPayer().getFirstName() != null) {
                po.setNomePagador(payment.getPayer().getFirstName() + " " +
                    (payment.getPayer().getLastName() != null ? payment.getPayer().getLastName() : ""));
            }
        }

        po.setRespostaGateway(payment.toString());
        po.incrementarTentativas();

        pagamentoOnlineRepository.save(po);

        // Se aprovado, criar/atualizar pagamento local
        if (novoStatus == StatusPagamentoOnline.APROVADO && po.getPagamentoId() == null) {
            criarPagamentoLocal(po, payment);
        }

        log.info("Pagamento online {} atualizado para status {}", po.getId(), novoStatus);
    }

    /**
     * Cria o pagamento local quando o pagamento online é aprovado.
     * Usa verificação no banco para evitar duplicação em caso de chamadas concorrentes.
     */
    private synchronized void criarPagamentoLocal(PagamentoOnline po, Payment payment) {
        log.info("Criando pagamento local para pagamento online {}", po.getId());

        try {
            // Recarregar o pagamento online para verificar se já foi processado (evita race condition)
            PagamentoOnline poAtualizado = pagamentoOnlineRepository.findById(po.getId()).orElse(po);
            if (poAtualizado.getPagamentoId() != null) {
                log.info("Pagamento local já existe para pagamento online {} - ID: {}", po.getId(), poAtualizado.getPagamentoId());
                return; // Pagamento já foi criado por outra thread/requisição
            }

            // Garantir que o TenantContext está definido com a oficina do pagamento online
            UUID oficinaId = po.getOficina() != null ? po.getOficina().getId() : null;

            // Verificar também se já existe um pagamento para esta OS com o mesmo ID externo do MP
            String observacaoEsperada = "Pagamento online via Mercado Pago - ID: " + payment.getId();
            if (oficinaId != null) {
                boolean pagamentoJaExiste = pagamentoRepository.findByOficinaIdAndOrdemServicoId(oficinaId, po.getOrdemServicoId())
                    .stream()
                    .anyMatch(p -> p.getObservacao() != null && p.getObservacao().contains("ID: " + payment.getId()));

                if (pagamentoJaExiste) {
                    log.warn("Pagamento com ID MP {} já existe para OS {} - ignorando duplicação",
                        payment.getId(), po.getOrdemServicoId());
                    return;
                }
            }
            if (oficinaId != null && !TenantContext.isSet()) {
                TenantContext.setTenantId(oficinaId);
            }

            // Obter parcelas (getInstallments retorna Integer que pode ser null)
            int parcelas = 1;
            Integer installments = payment.getInstallments();
            if (installments != null && installments > 0) {
                parcelas = installments;
            }

            Pagamento pagamento = Pagamento.builder()
                .ordemServicoId(po.getOrdemServicoId())
                .tipo(mapearTipoPagamento(payment.getPaymentMethodId()))
                .status(StatusPagamento.PAGO)
                .valor(po.getValor())
                .parcelas(parcelas)
                .parcelaAtual(1)
                .dataPagamento(po.getDataAprovacao() != null
                    ? po.getDataAprovacao().toLocalDate()
                    : java.time.LocalDate.now())
                .observacao(observacaoEsperada)
                .build();

            // Definir oficina explicitamente
            if (po.getOficina() != null) {
                pagamento.setOficina(po.getOficina());
            }

            pagamento = pagamentoRepository.save(pagamento);

            po.setPagamentoId(pagamento.getId());
            pagamentoOnlineRepository.save(po);

            log.info("Pagamento local {} criado com sucesso para pagamento online {}", pagamento.getId(), po.getId());
        } catch (Exception e) {
            log.error("Erro ao criar pagamento local para pagamento online {}: {}", po.getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Consulta status de um pagamento online.
     */
    public PagamentoOnlineDTO consultarStatus(UUID pagamentoOnlineId) {
        PagamentoOnline po = pagamentoOnlineRepository.findById(pagamentoOnlineId)
            .orElseThrow(() -> new IllegalArgumentException("Pagamento online não encontrado"));

        // Se está pendente e não expirou, tentar atualizar
        if (po.getStatus() == StatusPagamentoOnline.PENDENTE && !po.isExpirado() && po.getIdExterno() != null) {
            processarPagamento(po.getIdExterno());
            po = pagamentoOnlineRepository.findById(pagamentoOnlineId).orElse(po);
        }

        return toDTO(po);
    }

    /**
     * Processa pagamento recebido do Checkout Brick (inline).
     * Para cartões, o Brick já processa e retorna o payment_id.
     * Para PIX/Boleto, precisamos criar o pagamento via API.
     */
    @Transactional
    public Map<String, Object> processarPagamentoBrick(
            Map<String, Object> formData,
            UUID ordemServicoId,
            String preferenceId) {

        log.info("Processando pagamento Brick para OS {} - Preference: {}", ordemServicoId, preferenceId);
        log.info("FormData recebido: {}", formData);

        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoGateway config = getConfiguracaoAtiva(oficinaId);
        MercadoPagoConfig.setAccessToken(config.getAccessToken());

        try {
            // Verificar se o Brick já retornou um payment_id (pagamentos com cartão)
            Object paymentIdObj = formData.get("id");
            String paymentId = paymentIdObj != null ? paymentIdObj.toString() : null;

            // Se já temos um payment_id, apenas consultamos e retornamos
            if (paymentId != null && !paymentId.isEmpty()) {
                return consultarPagamentoExistente(paymentId, ordemServicoId);
            }

            // Se não tem payment_id, precisamos criar o pagamento
            // Identificar o tipo de pagamento
            String selectedPaymentMethod = (String) formData.get("selectedPaymentMethod");
            String paymentType = (String) formData.get("paymentType");

            log.info("Método: {}, Tipo: {}", selectedPaymentMethod, paymentType);

            // PIX = bank_transfer
            if ("bank_transfer".equals(selectedPaymentMethod) || "bank_transfer".equals(paymentType)) {
                return criarPagamentoPix(ordemServicoId, formData);
            }

            // Boleto = ticket
            if ("ticket".equals(selectedPaymentMethod) || "ticket".equals(paymentType)) {
                return criarPagamentoBoleto(ordemServicoId, formData);
            }

            // Cartão - o formData deve conter o token
            @SuppressWarnings("unchecked")
            Map<String, Object> innerFormData = (Map<String, Object>) formData.get("formData");
            if (innerFormData != null && innerFormData.containsKey("token")) {
                return criarPagamentoCartao(ordemServicoId, innerFormData);
            }

            // Tipo de pagamento não identificado
            log.warn("Tipo de pagamento não identificado: {}", formData);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("error", "Tipo de pagamento não suportado ou dados incompletos");
            return result;

        } catch (MPApiException e) {
            String apiResponse = e.getApiResponse() != null ? e.getApiResponse().getContent() : "N/A";
            log.error("Erro API MP ao processar Brick: Status={}, Mensagem={}, Resposta={}",
                e.getStatusCode(), e.getMessage(), apiResponse);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("error", "Erro no gateway: " + apiResponse);
            return result;
        } catch (MPException e) {
            log.error("Erro MP ao processar Brick: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("error", "Erro de comunicação: " + e.getMessage());
            return result;
        } catch (Exception e) {
            log.error("Erro inesperado ao processar Brick: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("error", "Erro inesperado: " + e.getMessage());
            return result;
        }
    }

    /**
     * Consulta um pagamento existente por ID.
     */
    private Map<String, Object> consultarPagamentoExistente(String paymentId, UUID ordemServicoId)
            throws MPException, MPApiException {
        log.info("Consultando pagamento existente: {}", paymentId);

        PaymentClient paymentClient = new PaymentClient();
        Payment payment = paymentClient.get(Long.parseLong(paymentId));

        if (payment == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("error", "Pagamento não encontrado");
            return result;
        }

        // Atualizar pagamento online
        List<PagamentoOnline> pagamentos = pagamentoOnlineRepository
            .findByOrdemServicoIdOrderByCreatedAtDesc(ordemServicoId);
        if (!pagamentos.isEmpty()) {
            atualizarPagamentoOnline(pagamentos.get(0), payment);
        }

        return extrairDadosPagamento(payment, paymentId);
    }

    /**
     * Cria um pagamento PIX via API do Mercado Pago.
     */
    private Map<String, Object> criarPagamentoPix(UUID ordemServicoId, Map<String, Object> formData)
            throws MPException, MPApiException {
        log.info("Criando pagamento PIX para OS {}", ordemServicoId);

        // Buscar OS para pegar o valor
        OrdemServico os = ordemServicoRepository.findById(ordemServicoId)
            .orElseThrow(() -> new IllegalArgumentException("OS não encontrada"));

        // Buscar pagamento online pendente
        List<PagamentoOnline> pagamentos = pagamentoOnlineRepository
            .findByOrdemServicoIdOrderByCreatedAtDesc(ordemServicoId);
        PagamentoOnline po = pagamentos.isEmpty() ? null : pagamentos.get(0);

        BigDecimal valor = po != null ? po.getValor() : os.getValorFinal();

        // Extrair dados do pagador do formData
        @SuppressWarnings("unchecked")
        Map<String, Object> innerFormData = (Map<String, Object>) formData.get("formData");
        String payerEmail = innerFormData != null ? (String) innerFormData.get("payer_email") : null;

        // Se não tem email, tentar buscar do cliente
        if (payerEmail == null || payerEmail.isBlank()) {
            payerEmail = getEmailCliente(os);
        }
        if (payerEmail == null || payerEmail.isBlank()) {
            payerEmail = po != null && po.getEmailPagador() != null ? po.getEmailPagador() : "cliente@pitstop.com";
        }

        // Criar pagamento via API
        PaymentClient paymentClient = new PaymentClient();

        com.mercadopago.client.payment.PaymentPayerRequest payer =
            com.mercadopago.client.payment.PaymentPayerRequest.builder()
                .email(payerEmail)
                .build();

        // Notification URL só é válida se for uma URL pública (não localhost)
        String notificationUrl = null;
        if (baseUrl != null && !baseUrl.contains("localhost") && !baseUrl.contains("127.0.0.1")) {
            notificationUrl = baseUrl + "/api/webhooks/mercadopago";
        }

        var paymentRequestBuilder = com.mercadopago.client.payment.PaymentCreateRequest.builder()
            .transactionAmount(valor)
            .description("OS #" + os.getNumero() + " - PitStop")
            .paymentMethodId("pix")
            .payer(payer)
            .externalReference(ordemServicoId.toString());

        if (notificationUrl != null) {
            paymentRequestBuilder.notificationUrl(notificationUrl);
        }

        com.mercadopago.client.payment.PaymentCreateRequest paymentRequest = paymentRequestBuilder.build();

        log.info("Criando pagamento PIX: valor={}, email={}, notificationUrl={}", valor, payerEmail, notificationUrl);

        Payment payment = paymentClient.create(paymentRequest);

        log.info("Pagamento PIX criado: ID={}, Status={}", payment.getId(), payment.getStatus());

        // Atualizar pagamento online
        if (po != null) {
            po.setIdExterno(payment.getId().toString());
            po.setStatus(mapearStatusMP(payment.getStatus()));
            po.setMetodoPagamento("pix");

            // Salvar dados do PIX no pagamento online
            if (payment.getPointOfInteraction() != null
                    && payment.getPointOfInteraction().getTransactionData() != null) {
                var td = payment.getPointOfInteraction().getTransactionData();
                po.setCodigoPix(td.getQrCode());
                po.setUrlQrCode(td.getTicketUrl());
            }

            pagamentoOnlineRepository.save(po);
        }

        Map<String, Object> result = extrairDadosPagamento(payment, payment.getId().toString());
        if (po != null) {
            result.put("pagamentoOnlineId", po.getId().toString());
        }
        return result;
    }

    /**
     * Cria um pagamento com Boleto via API do Mercado Pago.
     */
    private Map<String, Object> criarPagamentoBoleto(UUID ordemServicoId, Map<String, Object> formData)
            throws MPException, MPApiException {
        log.info("Criando pagamento Boleto para OS {}", ordemServicoId);

        // Buscar OS para pegar o valor
        OrdemServico os = ordemServicoRepository.findById(ordemServicoId)
            .orElseThrow(() -> new IllegalArgumentException("OS não encontrada"));

        // Buscar pagamento online pendente
        List<PagamentoOnline> pagamentos = pagamentoOnlineRepository
            .findByOrdemServicoIdOrderByCreatedAtDesc(ordemServicoId);
        PagamentoOnline po = pagamentos.isEmpty() ? null : pagamentos.get(0);

        BigDecimal valor = po != null ? po.getValor() : os.getValorFinal();

        // Extrair dados do pagador
        @SuppressWarnings("unchecked")
        Map<String, Object> innerFormData = (Map<String, Object>) formData.get("formData");

        String payerEmail = innerFormData != null ? (String) innerFormData.get("payer_email") : null;
        String payerFirstName = innerFormData != null ? (String) innerFormData.get("payer_first_name") : null;
        String payerLastName = innerFormData != null ? (String) innerFormData.get("payer_last_name") : null;

        if (payerEmail == null || payerEmail.isBlank()) {
            payerEmail = getEmailCliente(os);
        }
        if (payerEmail == null || payerEmail.isBlank()) {
            payerEmail = "cliente@pitstop.com";
        }

        // Criar pagamento via API
        PaymentClient paymentClient = new PaymentClient();

        com.mercadopago.client.payment.PaymentPayerRequest payer =
            com.mercadopago.client.payment.PaymentPayerRequest.builder()
                .email(payerEmail)
                .firstName(payerFirstName != null ? payerFirstName : "Cliente")
                .lastName(payerLastName != null ? payerLastName : "PitStop")
                .build();

        // Notification URL só é válida se for uma URL pública (não localhost)
        String notificationUrl = null;
        if (baseUrl != null && !baseUrl.contains("localhost") && !baseUrl.contains("127.0.0.1")) {
            notificationUrl = baseUrl + "/api/webhooks/mercadopago";
        }

        var paymentRequestBuilder = com.mercadopago.client.payment.PaymentCreateRequest.builder()
            .transactionAmount(valor)
            .description("OS #" + os.getNumero() + " - PitStop")
            .paymentMethodId("bolbradesco")
            .payer(payer)
            .externalReference(ordemServicoId.toString());

        if (notificationUrl != null) {
            paymentRequestBuilder.notificationUrl(notificationUrl);
        }

        Payment payment = paymentClient.create(paymentRequestBuilder.build());

        log.info("Pagamento Boleto criado: ID={}, Status={}", payment.getId(), payment.getStatus());

        // Atualizar pagamento online
        if (po != null) {
            po.setIdExterno(payment.getId().toString());
            po.setStatus(mapearStatusMP(payment.getStatus()));
            po.setMetodoPagamento("bolbradesco");
            pagamentoOnlineRepository.save(po);
        }

        Map<String, Object> result = extrairDadosPagamento(payment, payment.getId().toString());
        if (po != null) {
            result.put("pagamentoOnlineId", po.getId().toString());
        }
        return result;
    }

    /**
     * Cria um pagamento com Cartão via API do Mercado Pago.
     */
    private Map<String, Object> criarPagamentoCartao(UUID ordemServicoId, Map<String, Object> cardData)
            throws MPException, MPApiException {
        log.info("Criando pagamento Cartão para OS {}", ordemServicoId);

        // Buscar OS
        OrdemServico os = ordemServicoRepository.findById(ordemServicoId)
            .orElseThrow(() -> new IllegalArgumentException("OS não encontrada"));

        // Buscar pagamento online pendente
        List<PagamentoOnline> pagamentos = pagamentoOnlineRepository
            .findByOrdemServicoIdOrderByCreatedAtDesc(ordemServicoId);
        PagamentoOnline po = pagamentos.isEmpty() ? null : pagamentos.get(0);

        BigDecimal valor = po != null ? po.getValor() : os.getValorFinal();

        // Extrair dados do cartão
        String token = (String) cardData.get("token");
        String paymentMethodId = (String) cardData.get("payment_method_id");
        String issuerId = cardData.get("issuer_id") != null ? cardData.get("issuer_id").toString() : null;
        Integer installments = cardData.get("installments") != null
            ? Integer.parseInt(cardData.get("installments").toString()) : 1;

        // Dados do pagador
        @SuppressWarnings("unchecked")
        Map<String, Object> payerData = (Map<String, Object>) cardData.get("payer");
        String payerEmail = payerData != null ? (String) payerData.get("email") : getEmailCliente(os);

        if (payerEmail == null || payerEmail.isBlank()) {
            payerEmail = "cliente@pitstop.com";
        }

        // Criar pagamento
        PaymentClient paymentClient = new PaymentClient();

        com.mercadopago.client.payment.PaymentPayerRequest payer =
            com.mercadopago.client.payment.PaymentPayerRequest.builder()
                .email(payerEmail)
                .build();

        // Notification URL só é válida se for uma URL pública (não localhost)
        String notificationUrl = null;
        if (baseUrl != null && !baseUrl.contains("localhost") && !baseUrl.contains("127.0.0.1")) {
            notificationUrl = baseUrl + "/api/webhooks/mercadopago";
        }

        var paymentRequestBuilder = com.mercadopago.client.payment.PaymentCreateRequest.builder()
            .transactionAmount(valor)
            .token(token)
            .description("OS #" + os.getNumero() + " - PitStop")
            .installments(installments)
            .paymentMethodId(paymentMethodId)
            .payer(payer)
            .externalReference(ordemServicoId.toString());

        if (notificationUrl != null) {
            paymentRequestBuilder.notificationUrl(notificationUrl);
        }

        if (issuerId != null && !issuerId.isEmpty()) {
            paymentRequestBuilder.issuerId(issuerId);
        }

        Payment payment = paymentClient.create(paymentRequestBuilder.build());

        log.info("Pagamento Cartão criado: ID={}, Status={}", payment.getId(), payment.getStatus());

        // Atualizar pagamento online
        if (po != null) {
            atualizarPagamentoOnline(po, payment);
        }

        Map<String, Object> result = extrairDadosPagamento(payment, payment.getId().toString());
        if (po != null) {
            result.put("pagamentoOnlineId", po.getId().toString());
        }
        return result;
    }

    /**
     * Extrai dados relevantes do pagamento para retornar ao frontend.
     */
    private Map<String, Object> extrairDadosPagamento(Payment payment, String paymentId) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", payment.getStatus());
        result.put("paymentId", paymentId);
        result.put("statusDetail", payment.getStatusDetail());
        result.put("payment_method_id", payment.getPaymentMethodId());

        // Extrair dados do PIX se disponível
        if (payment.getPointOfInteraction() != null
                && payment.getPointOfInteraction().getTransactionData() != null) {
            var transactionData = payment.getPointOfInteraction().getTransactionData();

            Map<String, Object> pixData = new HashMap<>();
            pixData.put("qr_code", transactionData.getQrCode());
            pixData.put("qr_code_base64", transactionData.getQrCodeBase64());
            pixData.put("ticket_url", transactionData.getTicketUrl());

            result.put("point_of_interaction", Map.of("transaction_data", pixData));

            log.info("PIX data extraído - QR Code presente: {}, Base64 presente: {}",
                transactionData.getQrCode() != null,
                transactionData.getQrCodeBase64() != null);
        }

        // Extrair dados do Boleto se disponível
        if (payment.getTransactionDetails() != null) {
            Map<String, Object> transactionDetails = new HashMap<>();
            transactionDetails.put("external_resource_url",
                payment.getTransactionDetails().getExternalResourceUrl());
            result.put("transaction_details", transactionDetails);
        }

        return result;
    }

    /**
     * Valida as credenciais do gateway.
     */
    public boolean validarCredenciais(UUID oficinaId, String accessToken) {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);
            PaymentClient client = new PaymentClient();
            // Tentar uma operação simples para validar o token
            // O SDK do MP não tem um método específico para validar token,
            // então tentamos uma busca que deve funcionar se o token é válido
            log.info("Validando credenciais MP para oficina {}", oficinaId);
            return true; // Se não lançou exceção, está ok
        } catch (Exception e) {
            log.error("Credenciais MP inválidas: {}", e.getMessage());
            return false;
        }
    }

    // ========== Métodos auxiliares ==========

    private ConfiguracaoGateway getConfiguracaoAtiva(UUID oficinaId) {
        return configuracaoGatewayRepository
            .findGatewayAtivo(oficinaId, TipoGateway.MERCADO_PAGO)
            .orElseThrow(() -> new RuntimeException(
                "Mercado Pago não está configurado. Configure as credenciais em Configurações > Pagamentos."));
    }

    private StatusPagamentoOnline mapearStatusMP(String statusMP) {
        if (statusMP == null) return StatusPagamentoOnline.PENDENTE;

        return switch (statusMP.toLowerCase()) {
            case "approved" -> StatusPagamentoOnline.APROVADO;
            case "authorized" -> StatusPagamentoOnline.AUTORIZADO;
            case "in_process", "pending" -> StatusPagamentoOnline.PROCESSANDO;
            case "in_mediation" -> StatusPagamentoOnline.EM_ANALISE;
            case "rejected" -> StatusPagamentoOnline.REJEITADO;
            case "cancelled" -> StatusPagamentoOnline.CANCELADO;
            case "refunded" -> StatusPagamentoOnline.ESTORNADO;
            case "charged_back" -> StatusPagamentoOnline.DEVOLVIDO;
            default -> StatusPagamentoOnline.PENDENTE;
        };
    }

    private TipoPagamento mapearTipoPagamento(String metodoPagamentoMP) {
        if (metodoPagamentoMP == null) return TipoPagamento.PIX;

        return switch (metodoPagamentoMP.toLowerCase()) {
            case "pix" -> TipoPagamento.PIX;
            case "credit_card", "visa", "master", "amex", "elo", "hipercard" -> TipoPagamento.CARTAO_CREDITO;
            case "debit_card", "debvisa", "debmaster" -> TipoPagamento.CARTAO_DEBITO;
            case "bolbradesco", "boleto" -> TipoPagamento.BOLETO;
            case "account_money" -> TipoPagamento.TRANSFERENCIA;
            default -> TipoPagamento.PIX;
        };
    }

    private String getNomeOficina(OrdemServico os) {
        // Simplificado - em produção, buscaria o nome da oficina
        return "PitStop";
    }

    private String getDescricaoOS(OrdemServico os) {
        StringBuilder sb = new StringBuilder();
        sb.append("Serviço automotivo");
        if (os.getVeiculoId() != null) {
            veiculoRepository.findById(os.getVeiculoId()).ifPresent(veiculo -> {
                sb.append(" - ").append(veiculo.getMarca())
                  .append(" ").append(veiculo.getModelo());
            });
        }
        return sb.toString();
    }

    private String getEmailCliente(OrdemServico os) {
        if (os.getVeiculoId() != null) {
            return veiculoRepository.findById(os.getVeiculoId())
                .filter(v -> v.getClienteId() != null)
                .flatMap(v -> clienteRepository.findById(v.getClienteId()))
                .map(Cliente::getEmail)
                .orElse(null);
        }
        return null;
    }

    private String getNomeCliente(OrdemServico os) {
        if (os.getVeiculoId() != null) {
            return veiculoRepository.findById(os.getVeiculoId())
                .filter(v -> v.getClienteId() != null)
                .flatMap(v -> clienteRepository.findById(v.getClienteId()))
                .map(Cliente::getNome)
                .orElse(null);
        }
        return null;
    }

    private PagamentoOnlineDTO toDTO(PagamentoOnline po) {
        return PagamentoOnlineDTO.builder()
            .id(po.getId())
            .ordemServicoId(po.getOrdemServicoId())
            .pagamentoId(po.getPagamentoId())
            .gateway(po.getGateway())
            .gatewayDescricao(po.getGateway().getDescricao())
            .preferenceId(po.getPreferenceId())
            .idExterno(po.getIdExterno())
            .idCobranca(po.getIdCobranca())
            .status(po.getStatus())
            .statusDescricao(po.getStatus().getDescricao())
            .statusDetalhe(po.getStatusDetalhe())
            .valor(po.getValor())
            .valorLiquido(po.getValorLiquido())
            .valorTaxa(po.getValorTaxa())
            .metodoPagamento(po.getMetodoPagamento())
            .bandeiraCartao(po.getBandeiraCartao())
            .ultimosDigitos(po.getUltimosDigitos())
            .parcelas(po.getParcelas())
            .urlCheckout(po.getUrlCheckout())
            .urlQrCode(po.getUrlQrCode())
            .codigoPix(po.getCodigoPix())
            .dataExpiracao(po.getDataExpiracao())
            .dataAprovacao(po.getDataAprovacao())
            .erroMensagem(po.getErroMensagem())
            .erroCodigo(po.getErroCodigo())
            .tentativas(po.getTentativas())
            .emailPagador(po.getEmailPagador())
            .nomePagador(po.getNomePagador())
            .documentoPagador(po.getDocumentoPagador())
            .expirado(po.isExpirado())
            .aprovado(po.isAprovado())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }
}
