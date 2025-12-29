package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.domain.Plano;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.repository.PlanoRepository;
import com.pitstop.shared.exception.BusinessException;
import com.pitstop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing subscription plans.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanoService {

    private final PlanoRepository planoRepository;
    private final OficinaRepository oficinaRepository;

    /**
     * Get all plans ordered by display order.
     */
    @Transactional(readOnly = true)
    public List<PlanoDTO> findAll() {
        return planoRepository.findAllByOrderByOrdemExibicaoAsc()
                .stream()
                .map(PlanoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all active plans.
     */
    @Transactional(readOnly = true)
    public List<PlanoDTO> findAllActive() {
        return planoRepository.findByAtivoTrueOrderByOrdemExibicaoAsc()
                .stream()
                .map(PlanoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get visible plans for pricing page.
     */
    @Transactional(readOnly = true)
    public List<PlanoDTO> findVisiblePlans() {
        return planoRepository.findVisiblePlans()
                .stream()
                .map(PlanoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Find plan by ID.
     */
    @Transactional(readOnly = true)
    public PlanoDTO findById(UUID id) {
        Plano plano = planoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + id));
        return PlanoDTO.fromEntity(plano);
    }

    /**
     * Find plan by code.
     */
    @Transactional(readOnly = true)
    public PlanoDTO findByCodigo(String codigo) {
        Plano plano = planoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + codigo));
        return PlanoDTO.fromEntity(plano);
    }

    /**
     * Create a new plan.
     */
    @Transactional
    public PlanoDTO create(CreatePlanoRequest request) {
        // Validate unique code
        if (planoRepository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Já existe um plano com o código: " + request.getCodigo());
        }

        Plano plano = Plano.builder()
                .codigo(request.getCodigo())
                .nome(request.getNome())
                .descricao(request.getDescricao())
                .valorMensal(request.getValorMensal())
                .valorAnual(request.getValorAnual())
                .trialDias(request.getTrialDias())
                .limiteUsuarios(request.getLimiteUsuarios())
                .limiteOsMes(request.getLimiteOsMes())
                .limiteClientes(request.getLimiteClientes())
                .limiteEspacoMb(request.getLimiteEspacoMb())
                .limiteApiCalls(request.getLimiteApiCalls())
                .limiteWhatsappMensagens(request.getLimiteWhatsappMensagens())
                .limiteEmailsMes(request.getLimiteEmailsMes())
                .features(request.getFeatures() != null ? request.getFeatures() : getDefaultFeatures())
                .ativo(request.getAtivo())
                .visivel(request.getVisivel())
                .recomendado(request.getRecomendado())
                .corDestaque(request.getCorDestaque())
                .tagPromocao(request.getTagPromocao())
                .ordemExibicao(request.getOrdemExibicao())
                .build();

        // If this is marked as recommended, unmark others
        if (Boolean.TRUE.equals(request.getRecomendado())) {
            unmarkOtherRecommended(null);
        }

        plano = planoRepository.save(plano);

        log.info("Plano criado: {} - {}", plano.getCodigo(), plano.getNome());

        return PlanoDTO.fromEntity(plano);
    }

    /**
     * Update an existing plan.
     */
    @Transactional
    public PlanoDTO update(UUID id, UpdatePlanoRequest request) {
        Plano plano = planoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + id));

        // Validate unique code if changed
        if (request.getCodigo() != null && !request.getCodigo().equals(plano.getCodigo())) {
            if (planoRepository.existsByCodigoAndIdNot(request.getCodigo(), id)) {
                throw new BusinessException("Já existe um plano com o código: " + request.getCodigo());
            }
            plano.setCodigo(request.getCodigo());
        }

        // Update fields if provided
        if (request.getNome() != null) {
            plano.setNome(request.getNome());
        }
        if (request.getDescricao() != null) {
            plano.setDescricao(request.getDescricao());
        }
        if (request.getValorMensal() != null) {
            plano.setValorMensal(request.getValorMensal());
        }
        if (request.getValorAnual() != null) {
            plano.setValorAnual(request.getValorAnual());
        }
        if (request.getTrialDias() != null) {
            plano.setTrialDias(request.getTrialDias());
        }
        if (request.getLimiteUsuarios() != null) {
            plano.setLimiteUsuarios(request.getLimiteUsuarios());
        }
        if (request.getLimiteOsMes() != null) {
            plano.setLimiteOsMes(request.getLimiteOsMes());
        }
        if (request.getLimiteClientes() != null) {
            plano.setLimiteClientes(request.getLimiteClientes());
        }
        if (request.getLimiteEspacoMb() != null) {
            plano.setLimiteEspacoMb(request.getLimiteEspacoMb());
        }
        if (request.getLimiteApiCalls() != null) {
            plano.setLimiteApiCalls(request.getLimiteApiCalls());
        }
        if (request.getLimiteWhatsappMensagens() != null) {
            plano.setLimiteWhatsappMensagens(request.getLimiteWhatsappMensagens());
        }
        if (request.getLimiteEmailsMes() != null) {
            plano.setLimiteEmailsMes(request.getLimiteEmailsMes());
        }
        if (request.getFeatures() != null) {
            plano.setFeatures(request.getFeatures());
        }
        if (request.getAtivo() != null) {
            plano.setAtivo(request.getAtivo());
        }
        if (request.getVisivel() != null) {
            plano.setVisivel(request.getVisivel());
        }
        if (request.getRecomendado() != null) {
            if (Boolean.TRUE.equals(request.getRecomendado())) {
                unmarkOtherRecommended(id);
            }
            plano.setRecomendado(request.getRecomendado());
        }
        if (request.getCorDestaque() != null) {
            plano.setCorDestaque(request.getCorDestaque());
        }
        if (request.getTagPromocao() != null) {
            plano.setTagPromocao(request.getTagPromocao());
        }
        if (request.getOrdemExibicao() != null) {
            plano.setOrdemExibicao(request.getOrdemExibicao());
        }

        plano = planoRepository.save(plano);

        log.info("Plano atualizado: {} - {}", plano.getCodigo(), plano.getNome());

        return PlanoDTO.fromEntity(plano);
    }

    /**
     * Delete a plan (soft delete by setting ativo = false).
     */
    @Transactional
    public void delete(UUID id) {
        Plano plano = planoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + id));

        // Check if plan is in use by any workshop
        long count = oficinaRepository.countByPlano(PlanoAssinatura.valueOf(plano.getCodigo()));
        if (count > 0) {
            throw new BusinessException("Não é possível excluir plano em uso por " + count + " oficina(s). Desative-o em vez disso.");
        }

        plano.setAtivo(false);
        plano.setVisivel(false);
        planoRepository.save(plano);

        log.info("Plano desativado: {} - {}", plano.getCodigo(), plano.getNome());
    }

    /**
     * Toggle plan visibility.
     */
    @Transactional
    public PlanoDTO toggleVisibility(UUID id) {
        Plano plano = planoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + id));

        plano.setVisivel(!plano.getVisivel());
        plano = planoRepository.save(plano);

        log.info("Plano {} visibilidade alterada para: {}", plano.getCodigo(), plano.getVisivel());

        return PlanoDTO.fromEntity(plano);
    }

    /**
     * Change a workshop's plan.
     */
    @Transactional
    public void alterarPlanoOficina(UUID oficinaId, AlterarPlanoOficinaRequest request) {
        Oficina oficina = oficinaRepository.findById(oficinaId)
                .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada: " + oficinaId));

        // Validate the new plan exists
        PlanoAssinatura novoPlano;
        try {
            novoPlano = PlanoAssinatura.valueOf(request.getNovoPlano());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Plano inválido: " + request.getNovoPlano());
        }

        PlanoAssinatura planoAnterior = oficina.getPlano();

        // Update the plan
        oficina.setPlano(novoPlano);

        // Update pricing if not keeping old price
        if (!Boolean.TRUE.equals(request.getManterPrecoAntigo())) {
            Plano planoDB = planoRepository.findByCodigo(request.getNovoPlano())
                    .orElse(null);
            if (planoDB != null) {
                oficina.setValorMensalidade(planoDB.getValorMensal());
            } else {
                oficina.setValorMensalidade(novoPlano.getValorMensal());
            }
        }

        // Update subscription date if applying immediately
        if (Boolean.TRUE.equals(request.getAplicarImediatamente())) {
            oficina.setDataAssinatura(LocalDate.now());
            oficina.setDataVencimentoPlano(LocalDate.now().plusMonths(1));
        }

        oficinaRepository.save(oficina);

        log.info("Oficina {} plano alterado: {} -> {}. Motivo: {}",
                oficina.getNomeFantasia(), planoAnterior.getNome(), novoPlano.getNome(),
                request.getMotivo() != null ? request.getMotivo() : "Não informado");
    }

    /**
     * Get plan statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalPlanos", planoRepository.count());
        stats.put("planosAtivos", planoRepository.countByAtivoTrue());

        // Count workshops per plan
        Map<String, Long> workshopsPerPlan = new HashMap<>();
        for (PlanoAssinatura plano : PlanoAssinatura.values()) {
            workshopsPerPlan.put(plano.name(), oficinaRepository.countByPlano(plano));
        }
        stats.put("oficinasPerPlano", workshopsPerPlan);

        // Calculate MRR per plan
        Map<String, BigDecimal> mrrPerPlan = new HashMap<>();
        for (PlanoAssinatura plano : PlanoAssinatura.values()) {
            long count = oficinaRepository.countByPlano(plano);
            BigDecimal mrr = plano.getValorMensal().multiply(BigDecimal.valueOf(count));
            mrrPerPlan.put(plano.name(), mrr);
        }
        stats.put("mrrPerPlano", mrrPerPlan);

        return stats;
    }

    // =====================================
    // HELPER METHODS
    // =====================================

    private void unmarkOtherRecommended(UUID exceptId) {
        planoRepository.findByRecomendadoTrue().ifPresent(plano -> {
            if (exceptId == null || !plano.getId().equals(exceptId)) {
                plano.setRecomendado(false);
                planoRepository.save(plano);
            }
        });
    }

    private Map<String, Boolean> getDefaultFeatures() {
        Map<String, Boolean> features = new HashMap<>();
        features.put("emiteNotaFiscal", false);
        features.put("whatsappAutomatizado", false);
        features.put("manutencaoPreventiva", false);
        features.put("anexoImagensDocumentos", false);
        features.put("relatoriosAvancados", false);
        features.put("integracaoMercadoPago", false);
        features.put("suportePrioritario", false);
        features.put("backupAutomatico", true);
        return features;
    }
}
