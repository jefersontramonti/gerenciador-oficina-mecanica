package com.pitstop.manutencaopreventiva.service;

import com.pitstop.manutencaopreventiva.domain.TemplateManutencao;
import com.pitstop.manutencaopreventiva.dto.TemplateManutencaoRequestDTO;
import com.pitstop.manutencaopreventiva.dto.TemplateManutencaoResponseDTO;
import com.pitstop.manutencaopreventiva.mapper.ManutencaoMapper;
import com.pitstop.manutencaopreventiva.repository.TemplateManutencaoRepository;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TemplateManutencaoService {

    private final TemplateManutencaoRepository templateRepository;
    private final ManutencaoMapper mapper;

    /**
     * Lista templates disponíveis para a oficina (globais + próprios).
     */
    @Transactional(readOnly = true)
    public List<TemplateManutencaoResponseDTO> listarDisponiveis() {
        UUID oficinaId = TenantContext.getTenantId();
        return templateRepository.findDisponiveisParaOficina(oficinaId)
            .stream()
            .map(mapper::toTemplateResponse)
            .toList();
    }

    /**
     * Lista templates com filtros e paginação.
     */
    @Transactional(readOnly = true)
    public Page<TemplateManutencaoResponseDTO> listar(
            String tipoManutencao,
            String busca,
            Pageable pageable) {

        UUID oficinaId = TenantContext.getTenantId();
        Page<TemplateManutencao> templates = templateRepository.findByFilters(
            oficinaId, tipoManutencao, busca, pageable
        );
        return templates.map(mapper::toTemplateResponse);
    }

    /**
     * Lista templates globais.
     */
    @Transactional(readOnly = true)
    public List<TemplateManutencaoResponseDTO> listarGlobais() {
        return templateRepository.findTemplatesGlobais()
            .stream()
            .map(mapper::toTemplateResponse)
            .toList();
    }

    /**
     * Lista tipos de manutenção disponíveis.
     */
    @Transactional(readOnly = true)
    public List<String> listarTiposManutencao() {
        UUID oficinaId = TenantContext.getTenantId();
        return templateRepository.findTiposManutencao(oficinaId);
    }

    /**
     * Busca template por ID.
     */
    @Transactional(readOnly = true)
    public TemplateManutencaoResponseDTO buscarPorId(UUID id) {
        TemplateManutencao template = templateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Template não encontrado"));

        return mapper.toTemplateResponse(template);
    }

    /**
     * Cria template personalizado para a oficina.
     */
    public TemplateManutencaoResponseDTO criar(TemplateManutencaoRequestDTO request) {
        UUID oficinaId = TenantContext.getTenantId();

        Oficina oficina = new Oficina();
        oficina.setId(oficinaId);

        List<TemplateManutencao.PecaSugerida> pecasSugeridas = null;
        if (request.pecasSugeridas() != null) {
            pecasSugeridas = request.pecasSugeridas().stream()
                .map(p -> new TemplateManutencao.PecaSugerida(p.pecaId(), p.quantidade()))
                .toList();
        }

        TemplateManutencao template = TemplateManutencao.builder()
            .oficina(oficina)
            .nome(request.nome())
            .descricao(request.descricao())
            .tipoManutencao(request.tipoManutencao())
            .intervaloDias(request.intervaloDias())
            .intervaloKm(request.intervaloKm())
            .criterio(request.criterio())
            .antecedenciaDias(request.antecedenciaDias() != null ? request.antecedenciaDias() : 15)
            .antecedenciaKm(request.antecedenciaKm() != null ? request.antecedenciaKm() : 1000)
            .checklist(request.checklist())
            .pecasSugeridas(pecasSugeridas)
            .valorEstimado(request.valorEstimado())
            .tempoEstimadoMinutos(request.tempoEstimadoMinutos())
            .build();

        template = templateRepository.save(template);

        log.info("Template de manutenção criado: {}", template.getId());
        return mapper.toTemplateResponse(template);
    }

    /**
     * Atualiza template (apenas templates da própria oficina).
     */
    public TemplateManutencaoResponseDTO atualizar(UUID id, TemplateManutencaoRequestDTO request) {
        TemplateManutencao template = templateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Template não encontrado"));

        validarPermissaoEdicao(template);

        List<TemplateManutencao.PecaSugerida> pecasSugeridas = null;
        if (request.pecasSugeridas() != null) {
            pecasSugeridas = request.pecasSugeridas().stream()
                .map(p -> new TemplateManutencao.PecaSugerida(p.pecaId(), p.quantidade()))
                .toList();
        }

        template.setNome(request.nome());
        template.setDescricao(request.descricao());
        template.setTipoManutencao(request.tipoManutencao());
        template.setIntervaloDias(request.intervaloDias());
        template.setIntervaloKm(request.intervaloKm());
        template.setCriterio(request.criterio());
        template.setAntecedenciaDias(request.antecedenciaDias());
        template.setAntecedenciaKm(request.antecedenciaKm());
        template.setChecklist(request.checklist());
        template.setPecasSugeridas(pecasSugeridas);
        template.setValorEstimado(request.valorEstimado());
        template.setTempoEstimadoMinutos(request.tempoEstimadoMinutos());

        template = templateRepository.save(template);

        log.info("Template de manutenção atualizado: {}", template.getId());
        return mapper.toTemplateResponse(template);
    }

    /**
     * Soft delete do template (apenas templates da própria oficina).
     */
    public void deletar(UUID id) {
        TemplateManutencao template = templateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Template não encontrado"));

        validarPermissaoEdicao(template);

        template.setAtivo(false);
        templateRepository.save(template);

        log.info("Template de manutenção deletado (soft): {}", id);
    }

    private void validarPermissaoEdicao(TemplateManutencao template) {
        if (template.isGlobal()) {
            throw new IllegalStateException("Não é possível editar templates globais");
        }

        UUID oficinaId = TenantContext.getTenantId();
        if (!template.getOficina().getId().equals(oficinaId)) {
            throw new ResourceNotFoundException("Template não encontrado");
        }
    }
}
