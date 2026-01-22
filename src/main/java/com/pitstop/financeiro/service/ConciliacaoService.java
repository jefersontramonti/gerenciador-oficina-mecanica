package com.pitstop.financeiro.service;

import com.pitstop.financeiro.domain.*;
import com.pitstop.financeiro.dto.*;
import com.pitstop.financeiro.repository.*;
import com.pitstop.financeiro.util.OFXParser;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para conciliação bancária.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConciliacaoService {

    private final ExtratoBancarioRepository extratoRepository;
    private final TransacaoExtratoRepository transacaoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final OFXParser ofxParser;

    // Tolerância para matching de valores (em reais)
    private static final BigDecimal TOLERANCIA_VALOR = new BigDecimal("0.01");
    // Tolerância para matching de datas (em dias)
    private static final int TOLERANCIA_DIAS = 3;

    // ========== Importação de Extrato ==========

    /**
     * Importa um arquivo OFX/CSV.
     */
    @Transactional
    public ExtratoBancarioDTO importarExtrato(MultipartFile arquivo, UUID contaBancariaId) throws Exception {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("Importando extrato para oficina {}: {}", oficinaId, arquivo.getOriginalFilename());

        // Parsear o arquivo OFX
        OFXParser.OFXResult ofxResult = ofxParser.parse(arquivo.getInputStream());

        // Verificar se já existe (duplicata)
        if (extratoRepository.existsByOficinaIdAndArquivoHash(oficinaId, ofxResult.getFileHash())) {
            throw new IllegalArgumentException("Este arquivo já foi importado anteriormente");
        }

        // Criar extrato
        Oficina oficina = new Oficina();
        oficina.setId(oficinaId);

        ExtratoBancario extrato = ExtratoBancario.builder()
            .oficina(oficina)
            .arquivoNome(arquivo.getOriginalFilename())
            .arquivoHash(ofxResult.getFileHash())
            .tipoArquivo("OFX")
            .dataInicio(ofxResult.getStartDate() != null ? ofxResult.getStartDate() : LocalDate.now().minusMonths(1))
            .dataFim(ofxResult.getEndDate() != null ? ofxResult.getEndDate() : LocalDate.now())
            .saldoFinal(ofxResult.getBalanceAmount())
            .status(StatusExtrato.PENDENTE)
            .build();

        // Converter transações
        for (OFXParser.OFXTransaction ofxTx : ofxResult.getTransactions()) {
            TipoTransacaoBancaria tipo = ofxTx.getAmount().compareTo(BigDecimal.ZERO) >= 0
                ? TipoTransacaoBancaria.CREDITO
                : TipoTransacaoBancaria.DEBITO;

            TransacaoExtrato transacao = TransacaoExtrato.builder()
                .dataTransacao(ofxTx.getDatePosted() != null ? ofxTx.getDatePosted() : LocalDate.now())
                .tipo(tipo)
                .valor(ofxTx.getAmount().abs())
                .descricao(ofxTx.getName() != null ? ofxTx.getName() : ofxTx.getMemo())
                .identificadorBanco(ofxTx.getFitId())
                .referencia(ofxTx.getRefNum() != null ? ofxTx.getRefNum() : ofxTx.getCheckNum())
                .status(StatusConciliacao.NAO_CONCILIADA)
                .build();

            extrato.addTransacao(transacao);
        }

        extrato = extratoRepository.save(extrato);

        // Executar matching automático
        executarMatchingAutomatico(extrato);

        return toDTO(extrato);
    }

    // ========== Listagem e Consulta ==========

    /**
     * Lista extratos da oficina.
     */
    @Transactional(readOnly = true)
    public Page<ExtratoBancarioDTO> listarExtratos(Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return extratoRepository.findByOficinaId(oficinaId, pageable)
            .map(this::toDTO);
    }

    /**
     * Busca extrato por ID com transações.
     */
    @Transactional(readOnly = true)
    public ExtratoBancarioDTO buscarExtrato(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        ExtratoBancario extrato = extratoRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Extrato não encontrado: " + id));

        ExtratoBancarioDTO dto = toDTO(extrato);
        dto.setTransacoes(extrato.getTransacoes().stream()
            .map(this::toDTO)
            .collect(Collectors.toList()));

        return dto;
    }

    /**
     * Lista transações de um extrato com sugestões de conciliação.
     */
    @Transactional(readOnly = true)
    public List<TransacaoExtratoDTO> listarTransacoesComSugestoes(UUID extratoId) {
        UUID oficinaId = TenantContext.getTenantId();
        ExtratoBancario extrato = extratoRepository.findByIdAndOficinaId(extratoId, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Extrato não encontrado: " + extratoId));

        return extrato.getTransacoes().stream()
            .map(t -> {
                TransacaoExtratoDTO dto = toDTO(t);
                if (t.getStatus() == StatusConciliacao.NAO_CONCILIADA && t.isCredito()) {
                    dto.setSugestoes(buscarSugestoes(t));
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    // ========== Conciliação ==========

    /**
     * Concilia uma transação com um pagamento.
     */
    @Transactional
    public TransacaoExtratoDTO conciliarTransacao(UUID transacaoId, UUID pagamentoId) {
        UUID oficinaId = TenantContext.getTenantId();

        TransacaoExtrato transacao = transacaoRepository.findByIdAndOficinaId(transacaoId, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada: " + transacaoId));

        if (!transacao.podeSerConciliada()) {
            throw new IllegalStateException("Transação já foi conciliada ou ignorada");
        }

        Pagamento pagamento = pagamentoRepository.findByOficinaIdAndId(oficinaId, pagamentoId)
            .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado: " + pagamentoId));

        if (pagamento.getConciliado() != null && pagamento.getConciliado()) {
            throw new IllegalStateException("Pagamento já foi conciliado com outra transação");
        }

        // Conciliar transação
        transacao.conciliar(pagamento, "MANUAL");
        transacao = transacaoRepository.save(transacao);

        // Marcar pagamento como conciliado
        pagamento.setConciliado(true);
        pagamento.setTransacaoExtratoId(transacao.getId());
        pagamentoRepository.save(pagamento);

        // Atualizar contadores do extrato
        ExtratoBancario extrato = transacao.getExtrato();
        extrato.atualizarContadores();
        extratoRepository.save(extrato);

        return toDTO(transacao);
    }

    /**
     * Ignora uma transação.
     */
    @Transactional
    public TransacaoExtratoDTO ignorarTransacao(UUID transacaoId, String observacao) {
        UUID oficinaId = TenantContext.getTenantId();

        TransacaoExtrato transacao = transacaoRepository.findByIdAndOficinaId(transacaoId, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada: " + transacaoId));

        transacao.ignorar(observacao);
        transacao = transacaoRepository.save(transacao);

        // Atualizar contadores do extrato
        ExtratoBancario extrato = transacao.getExtrato();
        extrato.atualizarContadores();
        extratoRepository.save(extrato);

        return toDTO(transacao);
    }

    /**
     * Desconcilia uma transação.
     */
    @Transactional
    public TransacaoExtratoDTO desconciliarTransacao(UUID transacaoId) {
        UUID oficinaId = TenantContext.getTenantId();

        TransacaoExtrato transacao = transacaoRepository.findByIdAndOficinaId(transacaoId, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada: " + transacaoId));

        // Desmarcar pagamento se estava conciliado
        Pagamento pagamento = transacao.getPagamento();
        if (pagamento != null) {
            pagamento.setConciliado(false);
            pagamento.setTransacaoExtratoId(null);
            pagamentoRepository.save(pagamento);
        }

        transacao.desconciliar();
        transacao = transacaoRepository.save(transacao);

        // Atualizar contadores do extrato
        ExtratoBancario extrato = transacao.getExtrato();
        extrato.atualizarContadores();
        extratoRepository.save(extrato);

        return toDTO(transacao);
    }

    /**
     * Conciliação em lote.
     */
    @Transactional
    public Map<String, Object> conciliarEmLote(ConciliacaoRequestDTO.ConciliacaoLoteDTO request) {
        int conciliadas = 0;
        int ignoradas = 0;
        int erros = 0;

        // Conciliar
        if (request.getConciliacoes() != null) {
            for (ConciliacaoRequestDTO.ConciliarTransacaoDTO item : request.getConciliacoes()) {
                try {
                    conciliarTransacao(item.getTransacaoId(), item.getPagamentoId());
                    conciliadas++;
                } catch (Exception e) {
                    log.warn("Erro ao conciliar transação {}: {}", item.getTransacaoId(), e.getMessage());
                    erros++;
                }
            }
        }

        // Ignorar
        if (request.getTransacoesIgnorar() != null) {
            for (UUID transacaoId : request.getTransacoesIgnorar()) {
                try {
                    ignorarTransacao(transacaoId, "Ignorado em lote");
                    ignoradas++;
                } catch (Exception e) {
                    log.warn("Erro ao ignorar transação {}: {}", transacaoId, e.getMessage());
                    erros++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("conciliadas", conciliadas);
        result.put("ignoradas", ignoradas);
        result.put("erros", erros);

        return result;
    }

    // ========== Matching Automático ==========

    /**
     * Executa matching automático para todas as transações de crédito do extrato.
     */
    @Transactional
    public int executarMatchingAutomatico(ExtratoBancario extrato) {
        UUID oficinaId = extrato.getOficina().getId();
        int matched = 0;

        for (TransacaoExtrato transacao : extrato.getTransacoes()) {
            // Só matchear créditos não conciliados
            if (!transacao.isCredito() || !transacao.podeSerConciliada()) {
                continue;
            }

            List<TransacaoExtratoDTO.SugestaoConciliacaoDTO> sugestoes = buscarSugestoes(transacao);

            // Se há uma sugestão com score alto (>90), conciliar automaticamente
            if (!sugestoes.isEmpty() && sugestoes.get(0).getScore() >= 90) {
                TransacaoExtratoDTO.SugestaoConciliacaoDTO melhorSugestao = sugestoes.get(0);

                Pagamento pagamento = pagamentoRepository.findById(melhorSugestao.getPagamentoId()).orElse(null);
                if (pagamento != null && (pagamento.getConciliado() == null || !pagamento.getConciliado())) {
                    // Conciliar transação
                    transacao.conciliar(pagamento, "AUTO");
                    transacaoRepository.save(transacao);

                    // Marcar pagamento como conciliado
                    pagamento.setConciliado(true);
                    pagamento.setTransacaoExtratoId(transacao.getId());
                    pagamentoRepository.save(pagamento);

                    matched++;
                }
            }
        }

        extrato.atualizarContadores();
        extratoRepository.save(extrato);

        log.info("Matching automático: {} transações conciliadas automaticamente", matched);

        return matched;
    }

    /**
     * Busca sugestões de pagamentos para conciliar com uma transação.
     */
    private List<TransacaoExtratoDTO.SugestaoConciliacaoDTO> buscarSugestoes(TransacaoExtrato transacao) {
        UUID oficinaId = transacao.getOficina().getId();
        LocalDate dataTransacao = transacao.getDataTransacao();
        BigDecimal valor = transacao.getValor();

        // Buscar pagamentos com valor similar e data próxima
        LocalDate dataInicio = dataTransacao.minusDays(TOLERANCIA_DIAS);
        LocalDate dataFim = dataTransacao.plusDays(TOLERANCIA_DIAS);

        List<Object[]> pagamentos = pagamentoRepository.findPagamentosParaConciliacao(
            oficinaId,
            valor.subtract(TOLERANCIA_VALOR),
            valor.add(TOLERANCIA_VALOR),
            dataInicio,
            dataFim
        );

        List<TransacaoExtratoDTO.SugestaoConciliacaoDTO> sugestoes = new ArrayList<>();

        for (Object[] row : pagamentos) {
            UUID pagamentoId = (UUID) row[0];
            LocalDate dataPagamento = ((java.sql.Date) row[1]).toLocalDate();
            BigDecimal valorPagamento = (BigDecimal) row[2];
            String tipoPagamento = (String) row[3];
            Long osNumero = row[4] != null ? ((Number) row[4]).longValue() : null;
            String clienteNome = row[5] != null ? (String) row[5] : null;

            // Calcular score de similaridade
            double score = calcularScore(transacao, dataPagamento, valorPagamento);

            if (score > 50) {
                sugestoes.add(TransacaoExtratoDTO.SugestaoConciliacaoDTO.builder()
                    .pagamentoId(pagamentoId)
                    .dataPagamento(dataPagamento)
                    .valor(valorPagamento)
                    .tipoPagamento(tipoPagamento)
                    .osNumero(osNumero != null ? String.valueOf(osNumero) : null)
                    .clienteNome(clienteNome)
                    .score(score)
                    .motivoSugestao(gerarMotivoSugestao(transacao, dataPagamento, valorPagamento))
                    .build());
            }
        }

        // Ordenar por score decrescente
        sugestoes.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        return sugestoes.stream().limit(5).collect(Collectors.toList());
    }

    /**
     * Calcula score de similaridade entre transação e pagamento.
     */
    private double calcularScore(TransacaoExtrato transacao, LocalDate dataPagamento, BigDecimal valorPagamento) {
        double score = 0;

        // Score por valor (máx 60 pontos)
        BigDecimal diferencaValor = transacao.getValor().subtract(valorPagamento).abs();
        if (diferencaValor.compareTo(BigDecimal.ZERO) == 0) {
            score += 60;
        } else if (diferencaValor.compareTo(new BigDecimal("0.10")) <= 0) {
            score += 55;
        } else if (diferencaValor.compareTo(new BigDecimal("1.00")) <= 0) {
            score += 40;
        }

        // Score por data (máx 40 pontos)
        long diferencaDias = Math.abs(transacao.getDataTransacao().toEpochDay() - dataPagamento.toEpochDay());
        if (diferencaDias == 0) {
            score += 40;
        } else if (diferencaDias == 1) {
            score += 30;
        } else if (diferencaDias <= 3) {
            score += 20;
        } else if (diferencaDias <= 5) {
            score += 10;
        }

        return score;
    }

    /**
     * Gera descrição do motivo da sugestão.
     */
    private String gerarMotivoSugestao(TransacaoExtrato transacao, LocalDate dataPagamento, BigDecimal valorPagamento) {
        BigDecimal diferencaValor = transacao.getValor().subtract(valorPagamento).abs();
        long diferencaDias = Math.abs(transacao.getDataTransacao().toEpochDay() - dataPagamento.toEpochDay());

        if (diferencaValor.compareTo(BigDecimal.ZERO) == 0 && diferencaDias == 0) {
            return "Valor e data exatos";
        } else if (diferencaValor.compareTo(BigDecimal.ZERO) == 0) {
            return "Valor exato, " + diferencaDias + " dia(s) de diferença";
        } else if (diferencaDias == 0) {
            return "Data exata, R$ " + diferencaValor + " de diferença";
        } else {
            return "Valor similar (" + diferencaDias + " dias de diferença)";
        }
    }

    // ========== Conversão para DTO ==========

    private ExtratoBancarioDTO toDTO(ExtratoBancario extrato) {
        return ExtratoBancarioDTO.builder()
            .id(extrato.getId())
            .contaBancariaId(extrato.getContaBancaria() != null ? extrato.getContaBancaria().getId() : null)
            .contaBancariaNome(extrato.getContaBancaria() != null ? extrato.getContaBancaria().getNome() : null)
            .arquivoNome(extrato.getArquivoNome())
            .tipoArquivo(extrato.getTipoArquivo())
            .dataImportacao(extrato.getDataImportacao())
            .dataInicio(extrato.getDataInicio())
            .dataFim(extrato.getDataFim())
            .saldoInicial(extrato.getSaldoInicial())
            .saldoFinal(extrato.getSaldoFinal())
            .totalTransacoes(extrato.getTotalTransacoes())
            .totalConciliadas(extrato.getTotalConciliadas())
            .totalPendentes(extrato.getTotalTransacoes() - extrato.getTotalConciliadas())
            .percentualConciliado(extrato.getPercentualConciliado())
            .status(extrato.getStatus())
            .build();
    }

    private TransacaoExtratoDTO toDTO(TransacaoExtrato transacao) {
        return TransacaoExtratoDTO.builder()
            .id(transacao.getId())
            .extratoId(transacao.getExtrato().getId())
            .dataTransacao(transacao.getDataTransacao())
            .dataLancamento(transacao.getDataLancamento())
            .tipo(transacao.getTipo())
            .valor(transacao.getValor())
            .descricao(transacao.getDescricao())
            .identificadorBanco(transacao.getIdentificadorBanco())
            .referencia(transacao.getReferencia())
            .categoriaBanco(transacao.getCategoriaBanco())
            .status(transacao.getStatus())
            .pagamentoId(transacao.getPagamento() != null ? transacao.getPagamento().getId() : null)
            .dataConciliacao(transacao.getDataConciliacao())
            .metodoConciliacao(transacao.getMetodoConciliacao())
            .observacao(transacao.getObservacao())
            .build();
    }
}
