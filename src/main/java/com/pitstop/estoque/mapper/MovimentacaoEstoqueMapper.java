package com.pitstop.estoque.mapper;

import com.pitstop.estoque.domain.MovimentacaoEstoque;
import com.pitstop.estoque.domain.Peca;
import com.pitstop.estoque.dto.MovimentacaoEstoqueResponseDTO;
import com.pitstop.estoque.repository.PecaRepository;
import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.repository.UsuarioRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MapStruct mapper para conversão entre MovimentacaoEstoque entity e DTOs.
 * Requer acesso aos repositories para buscar dados relacionados.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Mapper(componentModel = "spring")
public abstract class MovimentacaoEstoqueMapper {

    @Autowired
    protected PecaRepository pecaRepository;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected OrdemServicoRepository ordemServicoRepository;

    /**
     * Converte entidade MovimentacaoEstoque para DTO de resposta.
     *
     * @param movimentacao entidade
     * @return DTO de resposta
     */
    @Mapping(source = "pecaId", target = "peca", qualifiedByName = "mapPeca")
    @Mapping(source = "usuarioId", target = "usuario", qualifiedByName = "mapUsuario")
    @Mapping(source = "ordemServicoId", target = "numeroOS", qualifiedByName = "mapNumeroOS")
    public abstract MovimentacaoEstoqueResponseDTO toResponseDTO(MovimentacaoEstoque movimentacao);

    /**
     * Mapeia pecaId para PecaResumoDTO.
     *
     * @param pecaId ID da peça
     * @return PecaResumoDTO
     */
    @Named("mapPeca")
    protected MovimentacaoEstoqueResponseDTO.PecaResumoDTO mapPeca(java.util.UUID pecaId) {
        return pecaRepository.findById(pecaId)
                .map(peca -> new MovimentacaoEstoqueResponseDTO.PecaResumoDTO(
                        peca.getId(),
                        peca.getCodigo(),
                        peca.getDescricao()
                ))
                .orElse(null);
    }

    /**
     * Mapeia usuarioId para UsuarioResumoDTO.
     *
     * @param usuarioId ID do usuário
     * @return UsuarioResumoDTO
     */
    @Named("mapUsuario")
    protected MovimentacaoEstoqueResponseDTO.UsuarioResumoDTO mapUsuario(java.util.UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(usuario -> new MovimentacaoEstoqueResponseDTO.UsuarioResumoDTO(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getEmail()
                ))
                .orElse(null);
    }

    /**
     * Mapeia ordemServicoId para número da OS.
     *
     * @param ordemServicoId ID da OS (pode ser null)
     * @return número da OS ou null
     */
    @Named("mapNumeroOS")
    protected Long mapNumeroOS(java.util.UUID ordemServicoId) {
        if (ordemServicoId == null) {
            return null;
        }
        return ordemServicoRepository.findById(ordemServicoId)
                .map(OrdemServico::getNumero)
                .orElse(null);
    }
}
