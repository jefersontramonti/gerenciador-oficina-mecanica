package com.pitstop.financeiro.service;

import com.pitstop.financeiro.domain.NotaFiscal;
import com.pitstop.financeiro.domain.StatusNotaFiscal;
import com.pitstop.financeiro.dto.NotaFiscalRequestDTO;
import com.pitstop.financeiro.dto.NotaFiscalResponseDTO;
import com.pitstop.financeiro.dto.NotaFiscalResumoDTO;
import com.pitstop.financeiro.mapper.NotaFiscalMapper;
import com.pitstop.financeiro.repository.NotaFiscalRepository;
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

/**
 * Service para lógica de negócio de Notas Fiscais.
 *
 * <p><strong>⚠️ IMPLEMENTAÇÃO BÁSICA - MVP ⚠️</strong></p>
 * <p>Esta implementação fornece apenas CRUD básico para Notas Fiscais.
 * Não inclui integração com SEFAZ (emissão, autorização, cancelamento).
 * A integração completa está planejada para Phase 3.</p>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotaFiscalService {

    private final NotaFiscalRepository notaFiscalRepository;
    private final NotaFiscalMapper notaFiscalMapper;

    /**
     * Cria uma nova nota fiscal (modo digitação).
     * Não emite pela SEFAZ, apenas registra no sistema.
     *
     * @param dto dados da nota fiscal
     * @return nota fiscal criada
     */
    @Transactional
    public NotaFiscalResponseDTO criar(NotaFiscalRequestDTO dto) {
        log.info("Criando nota fiscal para OS: {}", dto.ordemServicoId());

        UUID oficinaId = TenantContext.getTenantId();
        NotaFiscal notaFiscal = notaFiscalMapper.toEntity(dto);

        // Define status inicial como DIGITACAO
        notaFiscal.setStatus(StatusNotaFiscal.DIGITACAO);

        // Busca próximo número disponível para a série
        Long proximoNumero = notaFiscalRepository.findProximoNumero(oficinaId, dto.serie());
        notaFiscal.setNumero(proximoNumero);

        NotaFiscal salva = notaFiscalRepository.save(notaFiscal);

        log.info("Nota fiscal criada com sucesso. ID: {}, Número: {}, Série: {}",
                 salva.getId(), salva.getNumero(), salva.getSerie());

        return notaFiscalMapper.toResponseDTO(salva);
    }

    /**
     * Busca nota fiscal por ID.
     *
     * @param id ID da nota fiscal
     * @return nota fiscal encontrada
     */
    @Transactional(readOnly = true)
    public NotaFiscalResponseDTO buscarPorId(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        NotaFiscal notaFiscal = notaFiscalRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Nota fiscal não encontrada com ID: " + id));

        return notaFiscalMapper.toResponseDTO(notaFiscal);
    }

    /**
     * Lista todas as notas fiscais com paginação.
     *
     * @param pageable paginação
     * @return página de notas fiscais
     */
    @Transactional(readOnly = true)
    public Page<NotaFiscalResumoDTO> listar(Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return notaFiscalRepository.findByOficinaId(oficinaId, pageable)
            .map(notaFiscalMapper::toResumoDTO);
    }

    /**
     * Busca notas fiscais por ordem de serviço.
     *
     * @param ordemServicoId ID da OS
     * @return lista de notas fiscais
     */
    @Transactional(readOnly = true)
    public List<NotaFiscalResponseDTO> buscarPorOrdemServico(UUID ordemServicoId) {
        UUID oficinaId = TenantContext.getTenantId();
        return notaFiscalRepository.findByOficinaIdAndOrdemServicoId(oficinaId, ordemServicoId)
            .stream()
            .map(notaFiscalMapper::toResponseDTO)
            .toList();
    }

    /**
     * Busca notas fiscais por status.
     *
     * @param status status da nota
     * @param pageable paginação
     * @return página de notas fiscais
     */
    @Transactional(readOnly = true)
    public Page<NotaFiscalResumoDTO> buscarPorStatus(StatusNotaFiscal status, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return notaFiscalRepository.findByOficinaIdAndStatus(oficinaId, status, pageable)
            .map(notaFiscalMapper::toResumoDTO);
    }

    /**
     * Busca nota fiscal por número e série.
     *
     * @param numero número da nota
     * @param serie série da nota
     * @return nota fiscal encontrada
     */
    @Transactional(readOnly = true)
    public NotaFiscalResponseDTO buscarPorNumeroESerie(Long numero, Integer serie) {
        UUID oficinaId = TenantContext.getTenantId();
        NotaFiscal notaFiscal = notaFiscalRepository.findByOficinaIdAndNumeroAndSerie(oficinaId, numero, serie)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Nota fiscal não encontrada com número %d e série %d", numero, serie)
            ));

        return notaFiscalMapper.toResponseDTO(notaFiscal);
    }

    /**
     * Busca nota fiscal por chave de acesso.
     *
     * @param chaveAcesso chave de acesso da NFe
     * @return nota fiscal encontrada
     */
    @Transactional(readOnly = true)
    public NotaFiscalResponseDTO buscarPorChaveAcesso(String chaveAcesso) {
        UUID oficinaId = TenantContext.getTenantId();
        NotaFiscal notaFiscal = notaFiscalRepository.findByOficinaIdAndChaveAcesso(oficinaId, chaveAcesso)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Nota fiscal não encontrada com chave de acesso: " + chaveAcesso
            ));

        return notaFiscalMapper.toResponseDTO(notaFiscal);
    }

    /**
     * Atualiza uma nota fiscal.
     * Apenas notas em digitação podem ser alteradas.
     *
     * @param id ID da nota fiscal
     * @param dto dados atualizados
     * @return nota fiscal atualizada
     */
    @Transactional
    public NotaFiscalResponseDTO atualizar(UUID id, NotaFiscalRequestDTO dto) {
        log.info("Atualizando nota fiscal ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        NotaFiscal notaFiscal = notaFiscalRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Nota fiscal não encontrada com ID: " + id));

        // Valida se pode ser alterada
        if (notaFiscal.getStatus() != StatusNotaFiscal.DIGITACAO) {
            throw new IllegalStateException("Apenas notas em digitação podem ser alteradas");
        }

        // Atualiza campos permitidos
        notaFiscal.setValorTotal(dto.valorTotal());
        notaFiscal.setNaturezaOperacao(dto.naturezaOperacao());
        notaFiscal.setCfop(dto.cfop());
        notaFiscal.setInformacoesComplementares(dto.informacoesComplementares());
        notaFiscal.setDataEmissao(dto.dataEmissao());

        NotaFiscal atualizada = notaFiscalRepository.save(notaFiscal);

        log.info("Nota fiscal atualizada. ID: {}", atualizada.getId());
        return notaFiscalMapper.toResponseDTO(atualizada);
    }

    /**
     * Deleta uma nota fiscal (soft delete).
     * Apenas notas em digitação podem ser deletadas.
     *
     * @param id ID da nota fiscal
     */
    @Transactional
    public void deletar(UUID id) {
        log.info("Deletando nota fiscal ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        NotaFiscal notaFiscal = notaFiscalRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Nota fiscal não encontrada com ID: " + id));

        // Valida se pode ser deletada
        if (notaFiscal.getStatus() != StatusNotaFiscal.DIGITACAO) {
            throw new IllegalStateException("Apenas notas em digitação podem ser deletadas");
        }

        notaFiscalRepository.delete(notaFiscal);

        log.info("Nota fiscal deletada. ID: {}", id);
    }

    /**
     * Verifica se existe nota fiscal para uma OS.
     *
     * @param ordemServicoId ID da OS
     * @return true se existe
     */
    @Transactional(readOnly = true)
    public boolean existeNotaFiscalParaOS(UUID ordemServicoId) {
        UUID oficinaId = TenantContext.getTenantId();
        return notaFiscalRepository.existsByOficinaIdAndOrdemServicoId(oficinaId, ordemServicoId);
    }

    /**
     * Busca o próximo número disponível para uma série.
     *
     * @param serie série da nota
     * @return próximo número disponível
     */
    @Transactional(readOnly = true)
    public Long buscarProximoNumero(Integer serie) {
        UUID oficinaId = TenantContext.getTenantId();
        return notaFiscalRepository.findProximoNumero(oficinaId, serie);
    }
}
