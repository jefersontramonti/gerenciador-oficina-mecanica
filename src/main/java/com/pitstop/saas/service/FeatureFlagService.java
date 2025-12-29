package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.domain.FeatureFlag;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.repository.FeatureFlagRepository;
import com.pitstop.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FeatureFlagService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagService.class);

    private final FeatureFlagRepository featureFlagRepository;
    private final OficinaRepository oficinaRepository;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository, OficinaRepository oficinaRepository) {
        this.featureFlagRepository = featureFlagRepository;
        this.oficinaRepository = oficinaRepository;
    }

    // =====================================
    // CRUD Operations
    // =====================================

    @Transactional(readOnly = true)
    public List<FeatureFlagDTO> findAll() {
        return featureFlagRepository.findAllOrderByCategoriaAndNome()
                .stream()
                .map(FeatureFlagDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagDTO> findByCategoria(String categoria) {
        return featureFlagRepository.findByCategoria(categoria)
                .stream()
                .map(FeatureFlagDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> findAllCategorias() {
        return featureFlagRepository.findAllCategorias();
    }

    @Transactional(readOnly = true)
    public FeatureFlagDTO findById(UUID id) {
        return featureFlagRepository.findById(id)
                .map(FeatureFlagDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag não encontrada com ID: " + id));
    }

    @Transactional(readOnly = true)
    public FeatureFlagDTO findByCodigo(String codigo) {
        return featureFlagRepository.findByCodigo(codigo)
                .map(FeatureFlagDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag não encontrada com código: " + codigo));
    }

    public FeatureFlagDTO create(CreateFeatureFlagRequest request, UUID createdBy) {
        // Verificar se já existe
        if (featureFlagRepository.existsByCodigo(request.codigo())) {
            throw new IllegalArgumentException("Já existe uma feature flag com o código: " + request.codigo());
        }

        FeatureFlag entity = new FeatureFlag();
        entity.setCodigo(request.codigo());
        entity.setNome(request.nome());
        entity.setDescricao(request.descricao());
        entity.setHabilitadoGlobal(request.habilitadoGlobal() != null ? request.habilitadoGlobal() : false);
        entity.setHabilitadoPorPlano(request.habilitadoPorPlano() != null ? request.habilitadoPorPlano() : new HashMap<>());
        entity.setHabilitadoPorOficina(request.habilitadoPorOficina() != null
            ? request.habilitadoPorOficina().toArray(new UUID[0])
            : new UUID[0]);
        entity.setPercentualRollout(request.percentualRollout() != null ? request.percentualRollout() : 0);
        entity.setDataInicio(request.dataInicio());
        entity.setDataFim(request.dataFim());
        entity.setCategoria(request.categoria() != null ? request.categoria() : "GERAL");
        entity.setRequerAutorizacao(request.requerAutorizacao() != null ? request.requerAutorizacao() : false);
        entity.setCreatedBy(createdBy);

        FeatureFlag saved = featureFlagRepository.save(entity);
        logger.info("Feature flag criada: {} ({})", saved.getNome(), saved.getCodigo());

        return FeatureFlagDTO.fromEntity(saved);
    }

    public FeatureFlagDTO update(UUID id, UpdateFeatureFlagRequest request, UUID updatedBy) {
        FeatureFlag entity = featureFlagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag não encontrada com ID: " + id));

        if (request.nome() != null) {
            entity.setNome(request.nome());
        }
        if (request.descricao() != null) {
            entity.setDescricao(request.descricao());
        }
        if (request.habilitadoGlobal() != null) {
            entity.setHabilitadoGlobal(request.habilitadoGlobal());
        }
        if (request.habilitadoPorPlano() != null) {
            entity.setHabilitadoPorPlano(request.habilitadoPorPlano());
        }
        if (request.habilitadoPorOficina() != null) {
            entity.setHabilitadoPorOficina(request.habilitadoPorOficina().toArray(new UUID[0]));
        }
        if (request.percentualRollout() != null) {
            entity.setPercentualRollout(request.percentualRollout());
        }
        if (request.dataInicio() != null) {
            entity.setDataInicio(request.dataInicio());
        }
        if (request.dataFim() != null) {
            entity.setDataFim(request.dataFim());
        }
        if (request.categoria() != null) {
            entity.setCategoria(request.categoria());
        }
        if (request.requerAutorizacao() != null) {
            entity.setRequerAutorizacao(request.requerAutorizacao());
        }
        entity.setUpdatedBy(updatedBy);

        FeatureFlag saved = featureFlagRepository.save(entity);
        logger.info("Feature flag atualizada: {} ({})", saved.getNome(), saved.getCodigo());

        return FeatureFlagDTO.fromEntity(saved);
    }

    public void delete(UUID id) {
        if (!featureFlagRepository.existsById(id)) {
            throw new ResourceNotFoundException("FeatureFlag não encontrada com ID: " + id);
        }
        featureFlagRepository.deleteById(id);
        logger.info("Feature flag deletada: {}", id);
    }

    // =====================================
    // Toggle Operations
    // =====================================

    public FeatureFlagDTO toggle(UUID id, ToggleFeatureFlagRequest request, UUID updatedBy) {
        FeatureFlag entity = featureFlagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag não encontrada com ID: " + id));

        // Toggle global
        if (request.habilitadoGlobal() != null) {
            entity.setHabilitadoGlobal(request.habilitadoGlobal());
        }

        // Toggle planos
        if (request.planosHabilitar() != null) {
            Map<String, Boolean> planos = entity.getHabilitadoPorPlano();
            if (planos == null) planos = new HashMap<>();
            for (String plano : request.planosHabilitar()) {
                planos.put(plano, true);
            }
            entity.setHabilitadoPorPlano(planos);
        }
        if (request.planosDesabilitar() != null) {
            Map<String, Boolean> planos = entity.getHabilitadoPorPlano();
            if (planos == null) planos = new HashMap<>();
            for (String plano : request.planosDesabilitar()) {
                planos.put(plano, false);
            }
            entity.setHabilitadoPorPlano(planos);
        }

        // Toggle oficinas
        if (request.oficinasHabilitar() != null) {
            for (UUID oficinaId : request.oficinasHabilitar()) {
                entity.addOficinaHabilitada(oficinaId);
            }
        }
        if (request.oficinasDesabilitar() != null) {
            for (UUID oficinaId : request.oficinasDesabilitar()) {
                entity.removeOficinaHabilitada(oficinaId);
            }
        }

        // Update rollout
        if (request.percentualRollout() != null) {
            entity.setPercentualRollout(request.percentualRollout());
        }

        entity.setUpdatedBy(updatedBy);
        FeatureFlag saved = featureFlagRepository.save(entity);
        logger.info("Feature flag toggled: {} ({})", saved.getNome(), saved.getCodigo());

        return FeatureFlagDTO.fromEntity(saved);
    }

    public FeatureFlagDTO toggleGlobal(UUID id, boolean habilitado, UUID updatedBy) {
        FeatureFlag entity = featureFlagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag não encontrada com ID: " + id));

        entity.setHabilitadoGlobal(habilitado);
        entity.setUpdatedBy(updatedBy);

        FeatureFlag saved = featureFlagRepository.save(entity);
        logger.info("Feature flag {} toggle global: {}", saved.getCodigo(), habilitado);

        return FeatureFlagDTO.fromEntity(saved);
    }

    // =====================================
    // Feature Verification Logic
    // =====================================

    /**
     * Verifica se uma feature está habilitada para uma oficina específica.
     *
     * A verificação segue a ordem:
     * 1. Se a flag não está ativa (fora do período), retorna false
     * 2. Se está habilitada globalmente, retorna true
     * 3. Se a oficina específica está na lista de habilitadas, retorna true
     * 4. Se o plano da oficina está habilitado, retorna true
     * 5. Se há percentual de rollout, verifica se a oficina está no percentual
     * 6. Caso contrário, retorna false
     */
    @Transactional(readOnly = true)
    public boolean isEnabled(String featureCode, UUID oficinaId) {
        Optional<FeatureFlag> flagOpt = featureFlagRepository.findByCodigo(featureCode);

        if (flagOpt.isEmpty()) {
            logger.debug("Feature flag não encontrada: {}", featureCode);
            return false;
        }

        FeatureFlag flag = flagOpt.get();

        // 1. Verificar período de atividade
        if (!flag.isAtivo()) {
            logger.debug("Feature flag {} não está ativa (fora do período)", featureCode);
            return false;
        }

        // 2. Verificar habilitação global
        if (Boolean.TRUE.equals(flag.getHabilitadoGlobal())) {
            logger.debug("Feature flag {} está habilitada globalmente", featureCode);
            return true;
        }

        // 3. Verificar habilitação por oficina
        if (flag.isHabilitadoParaOficina(oficinaId)) {
            logger.debug("Feature flag {} está habilitada para oficina {}", featureCode, oficinaId);
            return true;
        }

        // 4. Verificar habilitação por plano
        Optional<Oficina> oficinaOpt = oficinaRepository.findById(oficinaId);
        if (oficinaOpt.isPresent()) {
            Oficina oficina = oficinaOpt.get();
            PlanoAssinatura plano = oficina.getPlano();

            if (plano != null && flag.isHabilitadoParaPlano(plano.name())) {
                logger.debug("Feature flag {} está habilitada para plano {}", featureCode, plano);
                return true;
            }
        }

        // 5. Verificar rollout percentual
        Integer rollout = flag.getPercentualRollout();
        if (rollout != null && rollout > 0) {
            // Usar hash da oficina para determinismo
            int hash = Math.abs(oficinaId.hashCode());
            int bucket = hash % 100;

            if (bucket < rollout) {
                logger.debug("Feature flag {} habilitada por rollout para oficina {}", featureCode, oficinaId);
                return true;
            }
        }

        logger.debug("Feature flag {} não habilitada para oficina {}", featureCode, oficinaId);
        return false;
    }

    /**
     * Retorna todas as features habilitadas para uma oficina específica.
     */
    @Transactional(readOnly = true)
    public OficinaFeatureFlagsDTO getOficinaFeatures(UUID oficinaId) {
        List<FeatureFlag> allFlags = featureFlagRepository.findAll();

        Map<String, Boolean> features = new HashMap<>();
        for (FeatureFlag flag : allFlags) {
            features.put(flag.getCodigo(), isEnabled(flag.getCodigo(), oficinaId));
        }

        return new OficinaFeatureFlagsDTO(oficinaId, features);
    }

    /**
     * Retorna estatísticas de uma feature flag.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFeatureStats(UUID featureId) {
        FeatureFlag flag = featureFlagRepository.findById(featureId)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag não encontrada com ID: " + featureId));

        Map<String, Object> stats = new HashMap<>();
        stats.put("codigo", flag.getCodigo());
        stats.put("nome", flag.getNome());
        stats.put("habilitadoGlobal", flag.getHabilitadoGlobal());
        stats.put("totalOficinasHabilitadas", flag.getHabilitadoPorOficina() != null
            ? flag.getHabilitadoPorOficina().length : 0);
        stats.put("planosHabilitados", flag.getHabilitadoPorPlano() != null
            ? flag.getHabilitadoPorPlano().entrySet().stream()
                .filter(e -> Boolean.TRUE.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList())
            : List.of());
        stats.put("percentualRollout", flag.getPercentualRollout());
        stats.put("ativo", flag.isAtivo());

        // Contar quantas oficinas teriam acesso baseado no plano
        if (flag.getHabilitadoPorPlano() != null) {
            long oficinasPorPlano = 0;
            for (Map.Entry<String, Boolean> entry : flag.getHabilitadoPorPlano().entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    try {
                        PlanoAssinatura plano = PlanoAssinatura.valueOf(entry.getKey());
                        oficinasPorPlano += oficinaRepository.countByPlano(plano);
                    } catch (IllegalArgumentException ignored) {
                        // Plano inválido, ignorar
                    }
                }
            }
            stats.put("totalOficinasPorPlano", oficinasPorPlano);
        }

        return stats;
    }
}
