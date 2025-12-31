package com.pitstop.financeiro.repository;

import com.pitstop.financeiro.domain.ConfiguracaoGateway;
import com.pitstop.financeiro.domain.TipoGateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfiguracaoGatewayRepository extends JpaRepository<ConfiguracaoGateway, UUID> {

    /**
     * Busca todas as configurações de gateway de uma oficina.
     */
    List<ConfiguracaoGateway> findByOficinaId(UUID oficinaId);

    /**
     * Busca apenas as configurações ativas de uma oficina.
     */
    List<ConfiguracaoGateway> findByOficinaIdAndAtivoTrue(UUID oficinaId);

    /**
     * Busca uma configuração específica de gateway para uma oficina.
     */
    Optional<ConfiguracaoGateway> findByOficinaIdAndTipoGateway(UUID oficinaId, TipoGateway tipoGateway);

    /**
     * Busca o gateway padrão da oficina.
     */
    Optional<ConfiguracaoGateway> findByOficinaIdAndPadraoTrue(UUID oficinaId);

    /**
     * Busca um gateway ativo e pronto para uso.
     */
    @Query("""
        SELECT c FROM ConfiguracaoGateway c
        WHERE c.oficina.id = :oficinaId
          AND c.tipoGateway = :tipoGateway
          AND c.ativo = true
          AND c.accessToken IS NOT NULL
        """)
    Optional<ConfiguracaoGateway> findGatewayAtivo(
        @Param("oficinaId") UUID oficinaId,
        @Param("tipoGateway") TipoGateway tipoGateway
    );

    /**
     * Verifica se existe um gateway configurado para a oficina.
     */
    boolean existsByOficinaIdAndTipoGateway(UUID oficinaId, TipoGateway tipoGateway);

    /**
     * Conta quantos gateways ativos a oficina possui.
     */
    long countByOficinaIdAndAtivoTrue(UUID oficinaId);

    /**
     * Busca configuração por ID com validação de tenant.
     * IMPORTANTE: Sempre usar este método ao invés de findById() para garantir isolamento multi-tenant.
     */
    @Query("SELECT c FROM ConfiguracaoGateway c WHERE c.oficina.id = :oficinaId AND c.id = :id")
    Optional<ConfiguracaoGateway> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);
}
