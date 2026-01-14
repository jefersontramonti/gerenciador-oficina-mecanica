package com.pitstop.anexo.repository;

import com.pitstop.anexo.domain.Anexo;
import com.pitstop.anexo.domain.EntidadeTipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para a entidade Anexo.
 *
 * <p>Todas as queries filtram por oficina_id para garantir
 * isolamento multi-tenant.</p>
 */
@Repository
public interface AnexoRepository extends JpaRepository<Anexo, UUID> {

    /**
     * Busca anexo por ID e oficina (multi-tenant).
     */
    Optional<Anexo> findByIdAndOficinaIdAndAtivoTrue(UUID id, UUID oficinaId);

    /**
     * Lista anexos de uma entidade específica.
     */
    @Query("""
        SELECT a FROM Anexo a
        WHERE a.oficina.id = :oficinaId
          AND a.entidadeTipo = :entidadeTipo
          AND a.entidadeId = :entidadeId
          AND a.ativo = true
        ORDER BY a.uploadedAt DESC
        """)
    List<Anexo> findByEntidade(
            @Param("oficinaId") UUID oficinaId,
            @Param("entidadeTipo") EntidadeTipo entidadeTipo,
            @Param("entidadeId") UUID entidadeId
    );

    /**
     * Lista anexos de uma entidade específica com paginação.
     */
    @Query("""
        SELECT a FROM Anexo a
        WHERE a.oficina.id = :oficinaId
          AND a.entidadeTipo = :entidadeTipo
          AND a.entidadeId = :entidadeId
          AND a.ativo = true
        """)
    Page<Anexo> findByEntidadePaged(
            @Param("oficinaId") UUID oficinaId,
            @Param("entidadeTipo") EntidadeTipo entidadeTipo,
            @Param("entidadeId") UUID entidadeId,
            Pageable pageable
    );

    /**
     * Conta anexos de uma entidade.
     */
    @Query("""
        SELECT COUNT(a) FROM Anexo a
        WHERE a.oficina.id = :oficinaId
          AND a.entidadeTipo = :entidadeTipo
          AND a.entidadeId = :entidadeId
          AND a.ativo = true
        """)
    long countByEntidade(
            @Param("oficinaId") UUID oficinaId,
            @Param("entidadeTipo") EntidadeTipo entidadeTipo,
            @Param("entidadeId") UUID entidadeId
    );

    /**
     * Calcula o total de bytes usados por uma oficina.
     */
    @Query("""
        SELECT COALESCE(SUM(a.tamanhoBytes), 0) FROM Anexo a
        WHERE a.oficina.id = :oficinaId
          AND a.ativo = true
        """)
    Long calcularUsoPorOficina(@Param("oficinaId") UUID oficinaId);

    /**
     * Conta total de anexos de uma oficina.
     */
    @Query("""
        SELECT COUNT(a) FROM Anexo a
        WHERE a.oficina.id = :oficinaId
          AND a.ativo = true
        """)
    long countByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Lista anexos órfãos (para limpeza).
     * Anexos inativos há mais de X dias.
     */
    @Query(value = """
        SELECT a.* FROM anexos a
        WHERE a.ativo = false
          AND a.uploaded_at < NOW() - INTERVAL '30 days'
        """, nativeQuery = true)
    List<Anexo> findAnexosOrfaos();

    /**
     * Verifica se existe anexo com determinado caminho.
     */
    boolean existsByCaminhoArquivo(String caminhoArquivo);

    /**
     * Lista anexos visíveis para cliente de uma entidade.
     * Usado na página pública de aprovação de orçamento.
     */
    @Query("""
        SELECT a FROM Anexo a
        WHERE a.oficina.id = :oficinaId
          AND a.entidadeTipo = :entidadeTipo
          AND a.entidadeId = :entidadeId
          AND a.ativo = true
          AND a.visivelParaCliente = true
        ORDER BY a.uploadedAt DESC
        """)
    List<Anexo> findVisiveisParaCliente(
            @Param("oficinaId") UUID oficinaId,
            @Param("entidadeTipo") EntidadeTipo entidadeTipo,
            @Param("entidadeId") UUID entidadeId
    );

    /**
     * Busca anexo visível para cliente por ID.
     * Usado na visualização pública.
     */
    @Query("""
        SELECT a FROM Anexo a
        WHERE a.id = :id
          AND a.oficina.id = :oficinaId
          AND a.entidadeTipo = :entidadeTipo
          AND a.entidadeId = :entidadeId
          AND a.ativo = true
          AND a.visivelParaCliente = true
        """)
    Optional<Anexo> findVisivelParaCliente(
            @Param("id") UUID id,
            @Param("oficinaId") UUID oficinaId,
            @Param("entidadeTipo") EntidadeTipo entidadeTipo,
            @Param("entidadeId") UUID entidadeId
    );
}
